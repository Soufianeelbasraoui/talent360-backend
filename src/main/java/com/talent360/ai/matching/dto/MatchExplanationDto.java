package com.talent360.ai.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchExplanationDto {
    private double score;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private boolean experienceMatch;
    private boolean educationMatch;
    private String summary;
}
