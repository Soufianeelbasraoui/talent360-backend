package com.talent360.candidate.mapper;

import com.talent360.candidate.dto.CandidateDocumentResponse;
import com.talent360.candidate.dto.CandidateResponse;
import com.talent360.candidate.dto.CandidateUpdateRequest;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.entity.CandidateDocument;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CandidateMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "skills", expression = "java(candidate.getSkillList())")
    CandidateResponse toResponse(Candidate candidate);

    CandidateDocumentResponse toDocumentResponse(CandidateDocument document);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "skillList", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CandidateUpdateRequest request, @MappingTarget Candidate candidate);
}
