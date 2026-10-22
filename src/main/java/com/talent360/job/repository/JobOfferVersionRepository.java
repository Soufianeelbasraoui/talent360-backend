package com.talent360.job.repository;

import com.talent360.job.entity.JobOfferVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOfferVersionRepository extends JpaRepository<JobOfferVersion, Long> {
    List<JobOfferVersion> findByJobOfferIdOrderByVersionNumberDesc(Long jobOfferId);
    long countByJobOfferId(Long jobOfferId);
}
