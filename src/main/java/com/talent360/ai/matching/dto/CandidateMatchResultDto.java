package com.talent360.ai.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateMatchResultDto {
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private MatchExplanationDto matchExplanation;
}
