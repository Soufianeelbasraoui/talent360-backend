package com.talent360.ai.jobdescription.dto;

import com.talent360.common.enums.ContractType;
import com.talent360.common.enums.ExperienceLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdGenerateRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Department is required")
    private String department;

    @NotNull(message = "Experience level is required (JUNIOR, MID, SENIOR)")
    private ExperienceLevel experienceLevel;

    private List<String> skills;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Contract type is required (CDI, CDD, FREELANCE, STAGE)")
    private ContractType contractType;
}
