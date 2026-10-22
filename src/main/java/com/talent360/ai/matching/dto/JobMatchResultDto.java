package com.talent360.ai.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchResultDto {
    private Long jobId;
    private String title;
    private String departmentName;
    private String location;
    private MatchExplanationDto matchExplanation;
}
