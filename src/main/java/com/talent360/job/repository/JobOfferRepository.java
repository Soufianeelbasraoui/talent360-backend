package com.talent360.job.repository;

import com.talent360.common.enums.ContractType;
import com.talent360.common.enums.ExperienceLevel;
import com.talent360.common.enums.JobStatus;
import com.talent360.common.enums.WorkMode;
import com.talent360.job.entity.JobOffer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long>, JpaSpecificationExecutor<JobOffer> {

    @Query("SELECT j FROM JobOffer j WHERE " +
           "(:status IS NULL OR j.status = :status) AND " +
           "(:departmentId IS NULL OR j.department.id = :departmentId) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:contractType IS NULL OR j.contractType = :contractType) AND " +
           "(:workMode IS NULL OR j.workMode = :workMode) AND " +
           "(:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) AND " +
           "(:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<JobOffer> searchJobs(
            @Param("status") JobStatus status,
            @Param("departmentId") Long departmentId,
            @Param("location") String location,
            @Param("contractType") ContractType contractType,
            @Param("workMode") WorkMode workMode,
            @Param("experienceLevel") ExperienceLevel experienceLevel,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<JobOffer> findByStatus(JobStatus status);

    long countByStatus(JobStatus status);
}
