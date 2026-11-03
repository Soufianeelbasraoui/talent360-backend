package com.talent360.ai.cv.entity;

import com.talent360.candidate.entity.CandidateDocument;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cv_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CVAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_document_id", nullable = false, unique = true)
    private CandidateDocument candidateDocument;

    @Column(name = "extracted_skills_json", columnDefinition = "TEXT")
    private String extractedSkillsJson;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(length = 255)
    private String education;

    @Column(name = "languages_json", columnDefinition = "TEXT")
    private String languagesJson;

    @Column(name = "certifications_json", columnDefinition = "TEXT")
    private String certificationsJson;

    @Column(name = "cv_score")
    @Builder.Default
    private Integer cvScore = 75;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "strengths_json", columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(name = "improvements_json", columnDefinition = "TEXT")
    private String improvementsJson;

    @Column(name = "analyzed_at", nullable = false)
    @Builder.Default
    private LocalDateTime analyzedAt = LocalDateTime.now();
}
