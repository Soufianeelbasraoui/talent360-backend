package com.talent360.job.mapper;

import com.talent360.job.dto.JobOfferRequest;
import com.talent360.job.dto.JobOfferResponse;
import com.talent360.job.entity.JobOffer;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface JobOfferMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "recruiterId", source = "recruiter.id")
    @Mapping(target = "recruiterEmail", source = "recruiter.email")
    @Mapping(target = "requiredSkills", expression = "java(jobOffer.getRequiredSkillList())")
    @Mapping(target = "versionCount", expression = "java(jobOffer.getVersions() != null ? jobOffer.getVersions().size() : 0)")
    @Mapping(target = "matchScore", ignore = true)
    JobOfferResponse toResponse(JobOffer jobOffer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "recruiter", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "versions", ignore = true)
    @Mapping(target = "requiredSkills", ignore = true)
    @Mapping(target = "requiredSkillList", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobOffer toEntity(JobOfferRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "recruiter", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "versions", ignore = true)
    @Mapping(target = "requiredSkills", ignore = true)
    @Mapping(target = "requiredSkillList", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(JobOfferRequest request, @MappingTarget JobOffer jobOffer);
}
