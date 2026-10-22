package com.talent360.ai.cv.repository;

import com.talent360.ai.cv.entity.CVAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CVAnalysisRepository extends JpaRepository<CVAnalysis, Long> {
    Optional<CVAnalysis> findByCandidateDocumentId(Long candidateDocumentId);
    boolean existsByCandidateDocumentId(Long candidateDocumentId);
}
