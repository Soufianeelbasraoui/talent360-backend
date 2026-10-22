package com.talent360.job.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.common.dto.PageResponse;
import com.talent360.common.enums.ContractType;
import com.talent360.common.enums.ExperienceLevel;
import com.talent360.common.enums.JobStatus;
import com.talent360.common.enums.WorkMode;
import com.talent360.department.entity.Department;
import com.talent360.department.repository.DepartmentRepository;
import com.talent360.job.dto.JobOfferRequest;
import com.talent360.job.dto.JobOfferResponse;
import com.talent360.job.entity.JobOffer;
import com.talent360.job.entity.JobOfferVersion;
import com.talent360.job.mapper.JobOfferMapper;
import com.talent360.job.repository.JobOfferRepository;
import com.talent360.job.repository.JobOfferVersionRepository;
import com.talent360.user.entity.User;
import com.talent360.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final JobOfferVersionRepository jobOfferVersionRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final JobOfferMapper jobOfferMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<JobOfferResponse> searchJobs(
            JobStatus status,
            Long departmentId,
            String location,
            ContractType contractType,
            WorkMode workMode,
            ExperienceLevel experienceLevel,
            String keyword,
            Pageable pageable
    ) {
        Page<JobOffer> page = jobOfferRepository.searchJobs(
                status,
                departmentId,
                location,
                contractType,
                workMode,
                experienceLevel,
                keyword,
                pageable
        );
        return PageResponse.from(page, jobOfferMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public JobOfferResponse getJobById(Long id) {
        JobOffer job = findJobById(id);
        return jobOfferMapper.toResponse(job);
    }

    @Transactional
    public JobOfferResponse createJob(JobOfferRequest request, String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Recruiter not found with email: " + recruiterEmail));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + request.getDepartmentId()));

        JobOffer job = jobOfferMapper.toEntity(request);
        job.setDepartment(department);
        job.setRecruiter(recruiter);
        job.setStatus(JobStatus.DRAFT);
        if (request.getRequiredSkills() != null) {
            job.setRequiredSkillList(request.getRequiredSkills());
        }

        JobOffer saved = jobOfferRepository.save(job);
        createSnapshotVersion(saved, "Initial job offer creation (DRAFT)");

        log.info("Created job offer with ID: {} by recruiter: {}", saved.getId(), recruiterEmail);
        return jobOfferMapper.toResponse(saved);
    }

    @Transactional
    public JobOfferResponse updateJob(Long id, JobOfferRequest request, String userEmail) {
        JobOffer job = findJobById(id);
        validateOwnershipOrAdmin(job, userEmail);

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + request.getDepartmentId()));

        jobOfferMapper.updateEntityFromRequest(request, job);
        job.setDepartment(department);
        if (request.getRequiredSkills() != null) {
            job.setRequiredSkillList(request.getRequiredSkills());
        }

        JobOffer updated = jobOfferRepository.save(job);
        String summary = request.getChangesSummary() != null ? request.getChangesSummary() : "Updated job details";
        createSnapshotVersion(updated, summary);

        log.info("Updated job offer ID: {}", id);
        return jobOfferMapper.toResponse(updated);
    }

    @Transactional
    public void deleteJob(Long id, String userEmail) {
        JobOffer job = findJobById(id);
        validateOwnershipOrAdmin(job, userEmail);
        jobOfferRepository.delete(job);
        log.info("Deleted job offer ID: {}", id);
    }

    @Transactional
    public JobOfferResponse publishJob(Long id, String userEmail) {
        JobOffer job = findJobById(id);
        validateOwnershipOrAdmin(job, userEmail);
        job.setStatus(JobStatus.OPEN);
        job.setPublishedAt(LocalDateTime.now());
        job.setClosedAt(null);

        JobOffer updated = jobOfferRepository.save(job);
        createSnapshotVersion(updated, "Published job offer to OPEN status");
        log.info("Published job offer ID: {}", id);
        return jobOfferMapper.toResponse(updated);
    }

    @Transactional
    public JobOfferResponse closeJob(Long id, String userEmail) {
        JobOffer job = findJobById(id);
        validateOwnershipOrAdmin(job, userEmail);
        job.setStatus(JobStatus.CLOSED);
        job.setClosedAt(LocalDateTime.now());

        JobOffer updated = jobOfferRepository.save(job);
        createSnapshotVersion(updated, "Closed job offer");
        log.info("Closed job offer ID: {}", id);
        return jobOfferMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobOfferResponse> getRecommendedJobs(String candidateEmail, Pageable pageable) {
        Candidate candidate = candidateRepository.findByUserEmail(candidateEmail)
                .orElseThrow(() -> new EntityNotFoundException("Candidate profile not found for email: " + candidateEmail));

        List<String> candidateSkills = candidate.getSkillList().stream()
                .map(String::toLowerCase)
                .toList();

        List<JobOffer> openJobs = jobOfferRepository.findByStatus(JobStatus.OPEN);

        List<JobOfferResponse> recommended = openJobs.stream()
                .map(job -> {
                    JobOfferResponse dto = jobOfferMapper.toResponse(job);
                    double score = calculateMatchScore(candidateSkills, candidate.getExperienceYears(), job);
                    dto.setMatchScore(score);
                    return dto;
                })
                .sorted(Comparator.comparingDouble(JobOfferResponse::getMatchScore).reversed())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), recommended.size());
        List<JobOfferResponse> pageContent = (start <= end && start < recommended.size())
                ? recommended.subList(start, end)
                : List.of();

        Page<JobOfferResponse> page = new PageImpl<>(pageContent, pageable, recommended.size());
        return PageResponse.from(page);
    }

    private double calculateMatchScore(List<String> candidateSkills, Integer experienceYears, JobOffer job) {
        List<String> jobSkills = job.getRequiredSkillList().stream()
                .map(String::toLowerCase)
                .toList();

        if (jobSkills.isEmpty()) {
            return 80.0;
        }

        long matchedSkillsCount = jobSkills.stream()
                .filter(candidateSkills::contains)
                .count();

        double skillRatio = (double) matchedSkillsCount / jobSkills.size();
        double skillScore = skillRatio * 70.0;

        double expScore = 15.0;
        if (experienceYears != null) {
            int requiredYears = switch (job.getExperienceLevel()) {
                case JUNIOR -> 1;
                case MID -> 3;
                case SENIOR -> 5;
            };
            if (experienceYears >= requiredYears) {
                expScore = 30.0;
            } else {
                expScore = Math.max(5.0, 30.0 * ((double) experienceYears / requiredYears));
            }
        }

        return Math.min(100.0, Math.round((skillScore + expScore) * 10.0) / 10.0);
    }

    private void createSnapshotVersion(JobOffer jobOffer, String changeSummary) {
        long versionCount = jobOfferVersionRepository.countByJobOfferId(jobOffer.getId());
        int nextVersion = (int) versionCount + 1;

        try {
            String snapshotJson = objectMapper.writeValueAsString(jobOfferMapper.toResponse(jobOffer));
            JobOfferVersion version = JobOfferVersion.builder()
                    .jobOffer(jobOffer)
                    .versionNumber(nextVersion)
                    .snapshotJson(snapshotJson)
                    .changesSummary(changeSummary)
                    .build();
            jobOfferVersionRepository.save(version);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize job snapshot for job ID {}", jobOffer.getId(), e);
        }
    }

    private JobOffer findJobById(Long id) {
        return jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found with id: " + id));
    }

    private void validateOwnershipOrAdmin(JobOffer job, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userEmail));

        boolean isAdmin = user.getRole() != null && user.getRole() == com.talent360.common.enums.Role.ADMIN;
        boolean isOwner = job.getRecruiter() != null && job.getRecruiter().getEmail().equalsIgnoreCase(userEmail);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to modify this job offer");
        }
    }
}
