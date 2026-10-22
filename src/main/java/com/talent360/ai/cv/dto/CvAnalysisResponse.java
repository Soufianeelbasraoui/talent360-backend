package com.talent360.ai.cv.dto;

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
public class CvAnalysisResponse {
    private Long id;
    private Long candidateDocumentId;
    private List<String> skills;
    private Integer experienceYears;
    private String education;
    private List<String> languages;
    private List<String> certifications;
    private LocalDateTime analyzedAt;
}
