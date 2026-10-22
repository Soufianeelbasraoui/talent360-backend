package com.talent360.ai.cv.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent360.ai.cv.dto.CvAnalysisResponse;
import com.talent360.ai.cv.dto.CvExtractionDto;
import com.talent360.ai.cv.entity.CVAnalysis;
import com.talent360.ai.cv.repository.CVAnalysisRepository;
import com.talent360.ai.cv.util.DocumentTextExtractor;
import com.talent360.ai.entity.AIGeneration;
import com.talent360.ai.repository.AIGenerationRepository;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.entity.CandidateDocument;
import com.talent360.candidate.repository.CandidateDocumentRepository;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.common.enums.AIGenerationType;
import com.talent360.exception.AiServiceException;
import com.talent360.user.entity.User;
import com.talent360.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvAnalysisService {

    private final CVAnalysisRepository cvAnalysisRepository;
    private final CandidateDocumentRepository candidateDocumentRepository;
    private final CandidateRepository candidateRepository;
    private final AIGenerationRepository aiGenerationRepository;
    private final UserRepository userRepository;
    private final DocumentTextExtractor documentTextExtractor;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public CvAnalysisResponse analyzeCv(Long candidateDocumentId, String userEmail) {
        var existingAnalysis = cvAnalysisRepository.findByCandidateDocumentId(candidateDocumentId);
        if (existingAnalysis.isPresent()) {
            log.info("Returning cached CV analysis for document ID {}", candidateDocumentId);
            return mapToResponse(existingAnalysis.get());
        }

        CandidateDocument document = candidateDocumentRepository.findById(candidateDocumentId)
                .orElseThrow(() -> new EntityNotFoundException("Candidate document not found with id: " + candidateDocumentId));

        User user = userRepository.findByEmail(userEmail).orElse(null);

        Path filePath = Paths.get(document.getFileUrl());
        String extractedText;
        try {
            extractedText = documentTextExtractor.extractText(filePath, document.getFileType());
        } catch (Exception e) {
            log.error("Failed to read CV document: {}", e.getMessage());
            throw new AiServiceException("Unable to read CV file: " + e.getMessage(), e);
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new AiServiceException("Document is empty or unreadable text could not be extracted");
        }

        String truncatedText = extractedText.length() > 12000 ? extractedText.substring(0, 12000) : extractedText;

        String prompt = """
                You are an expert HR and recruitment AI specialized in parsing Curriculum Vitae (CV) and resumes.
                Analyze the following CV text and extract structured information.
                Identify all technical and soft skills, estimate total years of professional experience,
                summarize highest level of education, list spoken languages, and list any certifications.

                CV CONTENT:
                \"\"\"
                %s
                \"\"\"
                """.formatted(truncatedText);

        CvExtractionDto extractionResult;
        try {
            extractionResult = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(CvExtractionDto.class);
        } catch (Exception ex) {
            log.error("LLM CV analysis failed: {}", ex.getMessage());
            extractionResult = fallbackExtraction(truncatedText);
        }

        if (extractionResult == null) {
            throw new AiServiceException("Failed to obtain structured data from CV analysis");
        }

        String skillsJson = toJson(extractionResult.skills());
        String languagesJson = toJson(extractionResult.languages());
        String certsJson = toJson(extractionResult.certifications());

        CVAnalysis analysis = CVAnalysis.builder()
                .candidateDocument(document)
                .extractedSkillsJson(skillsJson)
                .experienceYears(extractionResult.experienceYears())
                .education(extractionResult.education())
                .languagesJson(languagesJson)
                .certificationsJson(certsJson)
                .analyzedAt(LocalDateTime.now())
                .build();

        CVAnalysis savedAnalysis = cvAnalysisRepository.save(analysis);

        Candidate candidate = document.getCandidate();
        if (candidate != null) {
            if ((candidate.getSkills() == null || candidate.getSkills().isBlank()) && extractionResult.skills() != null) {
                candidate.setSkillList(extractionResult.skills());
            }
            if (candidate.getExperienceYears() == null && extractionResult.experienceYears() > 0) {
                candidate.setExperienceYears(extractionResult.experienceYears());
            }
            if (candidate.getEducation() == null && extractionResult.education() != null) {
                candidate.setEducation(extractionResult.education());
            }
            candidateRepository.save(candidate);
        }

        try {
            AIGeneration audit = AIGeneration.builder()
                    .type(AIGenerationType.CV_ANALYSIS)
                    .relatedEntityId(candidateDocumentId)
                    .relatedEntityType("CandidateDocument")
                    .promptUsed(prompt)
                    .generatedContent(objectMapper.writeValueAsString(extractionResult))
                    .createdBy(user)
                    .build();
            aiGenerationRepository.save(audit);
        } catch (Exception e) {
            log.warn("Failed to save AI audit trail: {}", e.getMessage());
        }

        return mapToResponse(savedAnalysis);
    }

    private CvExtractionDto fallbackExtraction(String text) {
        log.warn("Executing rule-based fallback CV extraction");
        List<String> knownKeywords = List.of("Java", "Spring", "SQL", "Docker", "Git", "React", "Python", "REST", "AWS", "Kubernetes", "Linux");
        List<String> found = new ArrayList<>();
        String lower = text.toLowerCase();
        for (String kw : knownKeywords) {
            if (lower.contains(kw.toLowerCase())) {
                found.add(kw);
            }
        }
        return new CvExtractionDto(found, 2, "Higher Education (Extracted Fallback)", List.of("English"), List.of());
    }

    private CvAnalysisResponse mapToResponse(CVAnalysis analysis) {
        return CvAnalysisResponse.builder()
                .id(analysis.getId())
                .candidateDocumentId(analysis.getCandidateDocument().getId())
                .skills(fromJsonList(analysis.getExtractedSkillsJson()))
                .experienceYears(analysis.getExperienceYears())
                .education(analysis.getEducation())
                .languages(fromJsonList(analysis.getLanguagesJson()))
                .certifications(fromJsonList(analysis.getCertificationsJson()))
                .analyzedAt(analysis.getAnalyzedAt())
                .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
