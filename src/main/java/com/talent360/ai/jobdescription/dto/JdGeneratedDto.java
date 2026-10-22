package com.talent360.ai.jobdescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdGeneratedDto {
    private String title;
    private String description;
    private List<String> responsibilities;
    private List<String> requiredSkills;
    private String profile;
    private String conditions;
}
