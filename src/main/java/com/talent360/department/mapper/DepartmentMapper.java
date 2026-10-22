package com.talent360.department.mapper;

import com.talent360.department.dto.DepartmentRequest;
import com.talent360.department.dto.DepartmentResponse;
import com.talent360.department.entity.Department;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DepartmentMapper {

    @Mapping(target = "jobOfferCount", expression = "java(department.getJobOffers() != null ? department.getJobOffers().size() : 0)")
    DepartmentResponse toResponse(Department department);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobOffers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Department toEntity(DepartmentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobOffers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(DepartmentRequest request, @MappingTarget Department department);
}
