package com.talent360.ai.matching.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent360.ai.matching.dto.MatchExplanationDto;
import com.talent360.ai.repository.AIGenerationRepository;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.common.enums.ContractType;
import com.talent360.common.enums.ExperienceLevel;
import com.talent360.common.enums.JobStatus;
import com.talent360.common.enums.WorkMode;
import com.talent360.job.entity.JobOffer;
import com.talent360.job.repository.JobOfferRepository;
import com.talent360.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private JobOfferRepository jobOfferRepository;

    @Mock
    private AIGenerationRepository aiGenerationRepository;

    @Mock
    private UserRepository userRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(
                candidateRepository, jobOfferRepository, aiGenerationRepository, userRepository, objectMapper
        );
    }

    @Test
    @DisplayName("Edge Case: 0 skills matched - skill score should be 0")
    void zeroSkillsMatched() {
        Candidate candidate = Candidate.builder()
                .skills("PHP, Laravel, Vue.js")
                .experienceYears(1)
                .education("Bachelor in CS")
                .location("Paris")
                .build();

        JobOffer job = JobOffer.builder()
                .requiredSkills("Java, Spring Boot, Docker, Kubernetes")
                .experienceLevel(ExperienceLevel.SENIOR)
                .location("London")
                .contractType(ContractType.CDI)
                .workMode(WorkMode.ONSITE)
                .status(JobStatus.OPEN)
                .build();

        MatchExplanationDto explanation = matchingService.computeExplanation(candidate, job);

        assertThat(explanation.getMatchingSkills()).isEmpty();
        assertThat(explanation.getMissingSkills()).containsExactly("Java", "Spring Boot", "Docker", "Kubernetes");
        assertThat(explanation.isExperienceMatch()).isFalse();
        assertThat(explanation.isEducationMatch()).isTrue();
        assertThat(explanation.getScore()).isLessThan(30.0);
    }

    @Test
    @DisplayName("Edge Case: 100% all skills matched and senior experience satisfied")
    void allSkillsMatched() {
        Candidate candidate = Candidate.builder()
                .skills("Java, Spring Boot, Docker, Kubernetes, MySQL")
                .experienceYears(6)
                .education("Master of Science")
                .location("Paris")
                .build();

        JobOffer job = JobOffer.builder()
                .requiredSkills("Java, Spring Boot, Docker")
                .experienceLevel(ExperienceLevel.SENIOR)
                .location("Paris")
                .contractType(ContractType.CDI)
                .workMode(WorkMode.HYBRID)
                .status(JobStatus.OPEN)
                .build();

        MatchExplanationDto explanation = matchingService.computeExplanation(candidate, job);

        assertThat(explanation.getMatchingSkills()).containsExactly("Java", "Spring Boot", "Docker");
        assertThat(explanation.getMissingSkills()).isEmpty();
        assertThat(explanation.isExperienceMatch()).isTrue();
        assertThat(explanation.isEducationMatch()).isTrue();
        assertThat(explanation.getScore()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Edge Case: Partial skills match (2 of 4) with mid-level experience")
    void partialSkillsMatch() {
        Candidate candidate = Candidate.builder()
                .skills("Java, React, Git")
                .experienceYears(3)
                .education("Degree in Software Engineering")
                .location("Lyon")
                .build();

        JobOffer job = JobOffer.builder()
                .requiredSkills("Java, React, AWS, Docker")
                .experienceLevel(ExperienceLevel.MID)
                .location("Paris")
                .contractType(ContractType.CDI)
                .workMode(WorkMode.REMOTE)
                .status(JobStatus.OPEN)
                .build();

        MatchExplanationDto explanation = matchingService.computeExplanation(candidate, job);

        assertThat(explanation.getMatchingSkills()).containsExactly("Java", "React");
        assertThat(explanation.getMissingSkills()).containsExactly("AWS", "Docker");
        assertThat(explanation.isExperienceMatch()).isTrue();
        assertThat(explanation.getScore()).isEqualTo(68.0);
    }
}
