package com.talent360.security;

import com.talent360.application.entity.Application;
import com.talent360.application.repository.ApplicationRepository;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.job.entity.JobOffer;
import com.talent360.job.repository.JobOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("ownershipSecurity")
@RequiredArgsConstructor
public class OwnershipSecurityService {

    private final JobOfferRepository jobOfferRepository;
    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public boolean isJobOwner(Long jobId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String currentEmail = authentication.getName();
        return jobOfferRepository.findById(jobId)
                .map(job -> job.getRecruiter() != null && currentEmail.equalsIgnoreCase(job.getRecruiter().getEmail()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isCandidateOwner(Long candidateId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String currentEmail = authentication.getName();
        return candidateRepository.findById(candidateId)
                .map(candidate -> candidate.getUser() != null && currentEmail.equalsIgnoreCase(candidate.getUser().getEmail()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isApplicationOwner(Long applicationId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String currentEmail = authentication.getName();
        return applicationRepository.findById(applicationId)
                .map(app -> app.getCandidate() != null &&
                            app.getCandidate().getUser() != null &&
                            currentEmail.equalsIgnoreCase(app.getCandidate().getUser().getEmail()))
                .orElse(false);
    }
}
