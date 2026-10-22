package com.talent360.application.service;

import com.talent360.application.dto.ApplicationRequest;
import com.talent360.application.dto.ApplicationResponse;
import com.talent360.application.entity.Application;
import com.talent360.application.mapper.ApplicationMapper;
import com.talent360.application.repository.ApplicationRepository;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.common.dto.PageResponse;
import com.talent360.common.enums.ApplicationStatus;
import com.talent360.common.enums.JobStatus;
import com.talent360.job.entity.JobOffer;
import com.talent360.job.repository.JobOfferRepository;
import com.talent360.user.entity.User;
import com.talent360.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;

    @Transactional
    public ApplicationResponse apply(ApplicationRequest request, String candidateEmail) {
        Candidate candidate = candidateRepository.findByUserEmail(candidateEmail)
                .orElseThrow(() -> new EntityNotFoundException("Candidate profile not found for email: " + candidateEmail));

        JobOffer jobOffer = jobOfferRepository.findById(request.getJobOfferId())
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found with id: " + request.getJobOfferId()));

        if (jobOffer.getStatus() != JobStatus.OPEN) {
            throw new IllegalStateException("Cannot apply to a job that is not OPEN. Current status: " + jobOffer.getStatus());
        }

        if (applicationRepository.existsByCandidateIdAndJobOfferId(candidate.getId(), jobOffer.getId())) {
            throw new IllegalStateException("You have already applied to this job offer");
        }

        double score = calculateMatchingScore(candidate, jobOffer);

        Application application = Application.builder()
                .candidate(candidate)
                .jobOffer(jobOffer)
                .status(ApplicationStatus.NEW)
                .matchingScore(score)
                .appliedAt(LocalDateTime.now())
                .build();

        Application saved = applicationRepository.save(application);
        log.info("Candidate ID {} successfully applied to job ID {} with score {}", candidate.getId(), jobOffer.getId(), score);

        return applicationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getMyApplications(String candidateEmail, Pageable pageable) {
        Page<Application> page = applicationRepository.findByCandidateUserEmail(candidateEmail, pageable);
        return PageResponse.from(page, applicationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getJobApplications(Long jobId, String userEmail, Pageable pageable) {
        JobOffer job = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found with id: " + jobId));

        validateRecruiterOwnership(job, userEmail);

        Page<Application> page = applicationRepository.findByJobOfferId(jobId, pageable);
        return PageResponse.from(page, applicationMapper::toResponse);
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(Long id, ApplicationStatus newStatus, String userEmail) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found with id: " + id));

        validateRecruiterOwnership(application.getJobOffer(), userEmail);

        validateStatusTransition(application.getStatus(), newStatus);

        application.setStatus(newStatus);
        Application updated = applicationRepository.save(application);
        log.info("Updated application ID {} status to {}", id, newStatus);

        return applicationMapper.toResponse(updated);
    }

    private void validateStatusTransition(ApplicationStatus current, ApplicationStatus next) {
        if (current == next) {
            return;
        }
        if (current == ApplicationStatus.HIRED || current == ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Cannot change status once an application has reached terminal state: " + current);
        }
    }

    private double calculateMatchingScore(Candidate candidate, JobOffer job) {
        List<String> jobSkills = job.getRequiredSkillList().stream()
                .map(String::toLowerCase)
                .toList();

        List<String> candidateSkills = candidate.getSkillList().stream()
                .map(String::toLowerCase)
                .toList();

        if (jobSkills.isEmpty()) {
            return 85.0;
        }

        long matches = jobSkills.stream().filter(candidateSkills::contains).count();
        double skillScore = ((double) matches / jobSkills.size()) * 70.0;

        double expScore = 15.0;
        if (candidate.getExperienceYears() != null) {
            int requiredYears = switch (job.getExperienceLevel()) {
                case JUNIOR -> 1;
                case MID -> 3;
                case SENIOR -> 5;
            };
            expScore = candidate.getExperienceYears() >= requiredYears ? 30.0 : Math.max(5.0, 30.0 * ((double) candidate.getExperienceYears() / requiredYears));
        }

        return Math.min(100.0, Math.round((skillScore + expScore) * 10.0) / 10.0);
    }

    private void validateRecruiterOwnership(JobOffer job, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userEmail));

        boolean isAdmin = user.getRole() != null && user.getRole() == com.talent360.common.enums.Role.ADMIN;
        boolean isOwner = job.getRecruiter() != null && job.getRecruiter().getEmail().equalsIgnoreCase(userEmail);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to view or manage applications for this job");
        }
    }
}
