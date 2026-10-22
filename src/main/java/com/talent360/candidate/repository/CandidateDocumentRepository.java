package com.talent360.candidate.repository;

import com.talent360.candidate.entity.CandidateDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, Long> {
    List<CandidateDocument> findByCandidateId(Long candidateId);
    Optional<CandidateDocument> findTopByCandidateIdOrderByUploadedAtDesc(Long candidateId);
}
