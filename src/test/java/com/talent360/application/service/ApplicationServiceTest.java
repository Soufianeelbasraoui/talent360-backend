package com.talent360.application.service;

import com.talent360.application.dto.ApplicationRequest;
import com.talent360.application.dto.ApplicationResponse;
import com.talent360.application.entity.Application;
import com.talent360.application.mapper.ApplicationMapper;
import com.talent360.application.repository.ApplicationRepository;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.common.enums.ApplicationStatus;
import com.talent360.common.enums.ContractType;
import com.talent360.common.enums.ExperienceLevel;
import com.talent360.common.enums.JobStatus;
import com.talent360.common.enums.WorkMode;
import com.talent360.job.entity.JobOffer;
import com.talent360.job.repository.JobOfferRepository;
import com.talent360.user.entity.User;
import com.talent360.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private JobOfferRepository jobOfferRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    @DisplayName("Duplicate application prevention: throws exception if candidate already applied to job")
    void applyDuplicateThrowsException() {
        Candidate candidate = Candidate.builder().id(10L).build();
        JobOffer job = JobOffer.builder().id(20L).status(JobStatus.OPEN).build();

        when(candidateRepository.findByUserEmail("cand@test.com")).thenReturn(Optional.of(candidate));
        when(jobOfferRepository.findById(20L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByCandidateIdAndJobOfferId(10L, 20L)).thenReturn(true);

        ApplicationRequest request = ApplicationRequest.builder().jobOfferId(20L).build();

        assertThatThrownBy(() -> applicationService.apply(request, "cand@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully submit application with computed matching score")
    void applySuccess() {
        Candidate candidate = Candidate.builder()
                .id(10L)
                .skills("Java, Spring Boot")
                .experienceYears(4)
                .build();

        JobOffer job = JobOffer.builder()
                .id(20L)
                .status(JobStatus.OPEN)
                .requiredSkills("Java, Spring Boot, Docker")
                .experienceLevel(ExperienceLevel.MID)
                .contractType(ContractType.CDI)
                .workMode(WorkMode.HYBRID)
                .build();

        Application savedApp = Application.builder()
                .id(100L)
                .candidate(candidate)
                .jobOffer(job)
                .status(ApplicationStatus.NEW)
                .matchingScore(76.7)
                .build();

        when(candidateRepository.findByUserEmail("cand@test.com")).thenReturn(Optional.of(candidate));
        when(jobOfferRepository.findById(20L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByCandidateIdAndJobOfferId(10L, 20L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(savedApp);
        when(applicationMapper.toResponse(savedApp)).thenReturn(
                ApplicationResponse.builder().id(100L).status(ApplicationStatus.NEW).matchingScore(76.7).build()
        );

        ApplicationResponse response = applicationService.apply(
                ApplicationRequest.builder().jobOfferId(20L).build(),
                "cand@test.com"
        );

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.NEW);
        assertThat(response.getMatchingScore()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Status transition rules: Cannot modify terminal status HIRED or REJECTED")
    void statusTransitionFromTerminalStateThrows() {
        User recruiter = User.builder().email("recruiter@talent.com").role(com.talent360.common.enums.Role.RECRUITER).build();
        JobOffer job = JobOffer.builder().recruiter(recruiter).build();
        Application application = Application.builder()
                .id(100L)
                .jobOffer(job)
                .status(ApplicationStatus.REJECTED)
                .build();

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail("recruiter@talent.com")).thenReturn(Optional.of(recruiter));

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(
                100L, ApplicationStatus.SCREENING, "recruiter@talent.com"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("terminal state");
    }
}
