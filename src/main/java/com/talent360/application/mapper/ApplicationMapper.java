package com.talent360.application.mapper;

import com.talent360.application.dto.ApplicationResponse;
import com.talent360.application.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "candidateId", source = "candidate.id")
    @Mapping(target = "candidateName", expression = "java(application.getCandidate() != null ? application.getCandidate().getFirstName() + \" \" + application.getCandidate().getLastName() : null)")
    @Mapping(target = "candidateEmail", expression = "java(application.getCandidate() != null && application.getCandidate().getUser() != null ? application.getCandidate().getUser().getEmail() : null)")
    @Mapping(target = "jobOfferId", source = "jobOffer.id")
    @Mapping(target = "jobOfferTitle", source = "jobOffer.title")
    ApplicationResponse toResponse(Application application);
}
