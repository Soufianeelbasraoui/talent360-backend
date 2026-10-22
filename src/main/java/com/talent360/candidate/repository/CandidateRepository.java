package com.talent360.candidate.repository;

import com.talent360.candidate.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    Optional<Candidate> findByUserId(Long userId);
    Optional<Candidate> findByUserEmail(String email);
    boolean existsByUserId(Long userId);
}
