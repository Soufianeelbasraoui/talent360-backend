package com.talent360.application.repository;

import com.talent360.application.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Page<Application> findByCandidateId(Long candidateId, Pageable pageable);

    Page<Application> findByCandidateUserEmail(String email, Pageable pageable);

    Page<Application> findByJobOfferId(Long jobOfferId, Pageable pageable);

    boolean existsByCandidateIdAndJobOfferId(Long candidateId, Long jobOfferId);

    Optional<Application> findByCandidateIdAndJobOfferId(Long candidateId, Long jobOfferId);

    long countByStatus(com.talent360.common.enums.ApplicationStatus status);

    java.util.List<Application> findTop10ByOrderByAppliedAtDesc();
}
