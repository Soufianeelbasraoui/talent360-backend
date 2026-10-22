package com.talent360.job.dto;

import com.talent360.common.enums.ContractType;
import com.talent360.common.enums.ExperienceLevel;
import com.talent360.common.enums.JobStatus;
import com.talent360.common.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobOfferResponse {
    private Long id;
    private String title;
    private String description;
    private Long departmentId;
    private String departmentName;
    private Long recruiterId;
    private String recruiterEmail;
    private String location;
    private ContractType contractType;
    private WorkMode workMode;
    private ExperienceLevel experienceLevel;
    private List<String> requiredSkills;
    private JobStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    private int versionCount;
    private Double matchScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
