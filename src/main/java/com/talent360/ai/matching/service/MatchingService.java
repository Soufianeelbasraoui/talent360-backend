package com.talent360.ai.matching.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent360.ai.entity.AIGeneration;
import com.talent360.ai.matching.dto.CandidateMatchResultDto;
import com.talent360.ai.matching.dto.JobMatchResultDto;
import com.talent360.ai.matching.dto.MatchExplanationDto;
import com.talent360.ai.repository.AIGenerationRepository;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.common.enums.AIGenerationType;
import com.talent360.common.enums.JobStatus;
import com.talent360.job.entity.JobOffer;
import com.talent360.job.repository.JobOfferRepository;
import com.talent360.user.entity.User;
import com.talent360.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final CandidateRepository candidateRepository;
    private final JobOfferRepository jobOfferRepository;
    private final AIGenerationRepository aiGenerationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<JobMatchResultDto> matchJobsForCandidate(String candidateEmail) {
        Candidate candidate = candidateRepository.findByUserEmail(candidateEmail)
                .orElseThrow(() -> new EntityNotFoundException("Candidate profile not found for email: " + candidateEmail));

        List<JobOffer> openJobs = jobOfferRepository.findByStatus(JobStatus.OPEN);

        List<JobMatchResultDto> results = openJobs.stream()
                .map(job -> {
                    MatchExplanationDto explanation = computeExplanation(candidate, job);
                    return JobMatchResultDto.builder()
                            .jobId(job.getId())
                            .title(job.getTitle())
                            .departmentName(job.getDepartment() != null ? job.getDepartment().getName() : null)
                            .location(job.getLocation())
                            .matchExplanation(explanation)
                            .build();
                })
                .sorted(Comparator.comparingDouble((JobMatchResultDto r) -> r.getMatchExplanation().getScore()).reversed())
                .toList();

        saveAuditTrail(AIGenerationType.MATCHING, candidate.getId(), "Candidate",
                "Match jobs for candidate: " + candidateEmail, results, candidate.getUser());

        return results;
    }

    @Transactional(readOnly = true)
    public List<CandidateMatchResultDto> matchCandidatesForJob(Long jobId, String recruiterEmail) {
        JobOffer job = jobOfferRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found with id: " + jobId));

        User recruiter = userRepository.findByEmail(recruiterEmail).orElse(null);

        List<Candidate> candidates = candidateRepository.findAll();

        List<CandidateMatchResultDto> results = candidates.stream()
                .map(candidate -> {
                    MatchExplanationDto explanation = computeExplanation(candidate, job);
                    String name = candidate.getFirstName() + " " + candidate.getLastName();
                    String email = candidate.getUser() != null ? candidate.getUser().getEmail() : null;

                    return CandidateMatchResultDto.builder()
                            .candidateId(candidate.getId())
                            .candidateName(name)
                            .candidateEmail(email)
                            .matchExplanation(explanation)
                            .build();
                })
                .sorted(Comparator.comparingDouble((CandidateMatchResultDto r) -> r.getMatchExplanation().getScore()).reversed())
                .toList();

        saveAuditTrail(AIGenerationType.MATCHING, jobId, "JobOffer",
                "Match candidates for job offer: " + jobId, results, recruiter);

        return results;
    }

    public MatchExplanationDto computeExplanation(Candidate candidate, JobOffer job) {
        List<String> jobSkills = job.getRequiredSkillList().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> candidateSkills = candidate.getSkillList().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> matchingSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String reqSkill : jobSkills) {
            boolean matched = candidateSkills.stream()
                    .anyMatch(cSkill -> cSkill.equalsIgnoreCase(reqSkill) ||
                                       cSkill.toLowerCase().contains(reqSkill.toLowerCase()) ||
                                       reqSkill.toLowerCase().contains(cSkill.toLowerCase()));
            if (matched) {
                matchingSkills.add(reqSkill);
            } else {
                missingSkills.add(reqSkill);
            }
        }

        double skillsScore;
        if (jobSkills.isEmpty()) {
            skillsScore = 60.0;
        } else {
            double ratio = (double) matchingSkills.size() / jobSkills.size();
            skillsScore = ratio * 60.0;
        }

        int requiredYears = switch (job.getExperienceLevel()) {
            case JUNIOR -> 1;
            case MID -> 3;
            case SENIOR -> 5;
        };
        int candYears = candidate.getExperienceYears() != null ? candidate.getExperienceYears() : 0;
        boolean experienceMatch = candYears >= requiredYears;
        double experienceScore;
        if (experienceMatch) {
            experienceScore = 25.0;
        } else if (candYears > 0) {
            experienceScore = 25.0 * ((double) candYears / requiredYears);
        } else {
            experienceScore = 0.0;
        }

        boolean educationMatch = candidate.getEducation() != null && !candidate.getEducation().isBlank();
        double educationScore = educationMatch ? 10.0 : 4.0;

        double preferencesScore = 3.0;
        if (candidate.getLocation() != null && job.getLocation() != null &&
            candidate.getLocation().equalsIgnoreCase(job.getLocation())) {
            preferencesScore = 5.0;
        }

        double totalScore = Math.min(100.0, Math.round((skillsScore + experienceScore + educationScore + preferencesScore) * 10.0) / 10.0);

        String summary = String.format("Candidate matches %d of %d required skills (%.0f%%), %s experience requirements (%d vs %d years required).",
                matchingSkills.size(), jobSkills.size(),
                jobSkills.isEmpty() ? 100.0 : ((double) matchingSkills.size() / jobSkills.size()) * 100.0,
                experienceMatch ? "satisfies" : "partially meets",
                candYears, requiredYears);

        return MatchExplanationDto.builder()
                .score(totalScore)
                .matchingSkills(matchingSkills)
                .missingSkills(missingSkills)
                .experienceMatch(experienceMatch)
                .educationMatch(educationMatch)
                .summary(summary)
                .build();
    }

    private void saveAuditTrail(AIGenerationType type, Long entityId, String entityType,
                                String prompt, Object result, User user) {
        try {
            AIGeneration audit = AIGeneration.builder()
                    .type(type)
                    .relatedEntityId(entityId)
                    .relatedEntityType(entityType)
                    .promptUsed(prompt)
                    .generatedContent(objectMapper.writeValueAsString(result))
                    .createdBy(user)
                    .build();
            aiGenerationRepository.save(audit);
        } catch (Exception e) {
            log.warn("Could not save matching audit record: {}", e.getMessage());
        }
    }
}
