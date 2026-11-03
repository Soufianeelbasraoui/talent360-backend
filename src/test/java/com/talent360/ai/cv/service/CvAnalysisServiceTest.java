package com.talent360.ai.cv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent360.ai.cv.dto.CvAnalysisResponse;
import com.talent360.ai.cv.dto.CvExtractionDto;
import com.talent360.ai.cv.entity.CVAnalysis;
import com.talent360.ai.cv.repository.CVAnalysisRepository;
import com.talent360.ai.cv.util.DocumentTextExtractor;
import com.talent360.ai.repository.AIGenerationRepository;
import com.talent360.candidate.entity.Candidate;
import com.talent360.candidate.entity.CandidateDocument;
import com.talent360.candidate.repository.CandidateDocumentRepository;
import com.talent360.candidate.repository.CandidateRepository;
import com.talent360.user.entity.User;
import com.talent360.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CvAnalysisServiceTest {

    @Mock
    private CVAnalysisRepository cvAnalysisRepository;

    @Mock
    private CandidateDocumentRepository candidateDocumentRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private AIGenerationRepository aiGenerationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentTextExtractor documentTextExtractor;

    @Mock
    private ChatClient chatClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CvAnalysisService cvAnalysisService;

    @BeforeEach
    void setUp() {
        cvAnalysisService = new CvAnalysisService(
                cvAnalysisRepository, candidateDocumentRepository, candidateRepository,
                aiGenerationRepository, userRepository, documentTextExtractor, chatClient, objectMapper
        );
    }

    @Test
    @DisplayName("Should return cached CV analysis if already analyzed")
    void returnCachedCvAnalysis() {
        CandidateDocument doc = CandidateDocument.builder().id(50L).build();
        CVAnalysis cached = CVAnalysis.builder()
                .id(1L)
                .candidateDocument(doc)
                .summary("Experienced software engineer")
                .extractedSkillsJson("[\"Java\", \"Spring\"]")
                .experienceYears(4)
                .education("BS Computer Science")
                .build();

        when(cvAnalysisRepository.findByCandidateDocumentId(50L)).thenReturn(Optional.of(cached));

        CvAnalysisResponse response = cvAnalysisService.analyzeCv(50L, "test@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSkills()).containsExactly("Java", "Spring");
        assertThat(response.getExperienceYears()).isEqualTo(4);

        verify(documentTextExtractor, never()).extractText(any(), any());
        verifyNoInteractions(chatClient);
    }

    @Test
    @DisplayName("Fallback parsing extracts skills when LLM call fails")
    void fallbackParsingWhenLlmFails() {
        Candidate candidate = Candidate.builder().id(10L).build();
        CandidateDocument doc = CandidateDocument.builder()
                .id(50L)
                .candidate(candidate)
                .fileUrl("uploads/sample_cv.pdf")
                .fileType("application/pdf")
                .build();

        when(cvAnalysisRepository.findByCandidateDocumentId(50L)).thenReturn(Optional.empty());
        when(candidateDocumentRepository.findById(50L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(User.builder().build()));
        when(documentTextExtractor.extractText(any(Path.class), any()))
                .thenReturn("Experienced Software Engineer with Java, Spring, Docker and Git experience.");

        when(chatClient.prompt()).thenThrow(new RuntimeException("OpenAI API unreachable"));

        CVAnalysis savedAnalysis = CVAnalysis.builder()
                .id(2L)
                .candidateDocument(doc)
                .extractedSkillsJson("[\"Java\", \"Spring\", \"Docker\", \"Git\"]")
                .experienceYears(2)
                .education("Higher Education (Extracted Fallback)")
                .build();

        when(cvAnalysisRepository.save(any(CVAnalysis.class))).thenReturn(savedAnalysis);

        CvAnalysisResponse response = cvAnalysisService.analyzeCv(50L, "recruiter@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getSkills()).contains("Java", "Spring", "Docker", "Git");
        verify(cvAnalysisRepository, times(1)).save(any(CVAnalysis.class));
        verify(candidateRepository, times(1)).save(any(Candidate.class));
    }
}
