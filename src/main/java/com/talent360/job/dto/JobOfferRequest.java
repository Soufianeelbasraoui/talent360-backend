package com.talent360.job.dto;

import com.talent360.common.enums.ContractType;
import com.talent360.common.enums.ExperienceLevel;
import com.talent360.common.enums.WorkMode;
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
public class JobOfferRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Job description is required")
    private String description;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Contract type is required (CDI, CDD, FREELANCE, STAGE)")
    private ContractType contractType;

    @NotNull(message = "Work mode is required (REMOTE, HYBRID, ONSITE)")
    private WorkMode workMode;

    @NotNull(message = "Experience level is required (JUNIOR, MID, SENIOR)")
    private ExperienceLevel experienceLevel;

    private List<String> requiredSkills;
    private String changesSummary;
}
