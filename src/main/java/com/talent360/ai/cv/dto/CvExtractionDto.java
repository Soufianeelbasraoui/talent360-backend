package com.talent360.ai.cv.dto;

import java.util.List;

public record CvExtractionDto(
        List<String> skills,
        int experienceYears,
        String education,
        List<String> languages,
        List<String> certifications,
        int cvScore,
        String summary,
        List<String> strengths,
        List<String> improvements
) {
}
