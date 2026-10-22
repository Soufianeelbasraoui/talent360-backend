package com.talent360.application.dto;

import com.talent360.common.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private Long jobOfferId;
    private String jobOfferTitle;
    private ApplicationStatus status;
    private Double matchingScore;
    private LocalDateTime appliedAt;
}
