package com.talent360.job.controller;

import com.talent360.common.dto.PageResponse;
import com.talent360.common.enums.ContractType;
import com.talent360.common.enums.ExperienceLevel;
import com.talent360.common.enums.JobStatus;
import com.talent360.common.enums.WorkMode;
import com.talent360.job.dto.JobOfferRequest;
import com.talent360.job.dto.JobOfferResponse;
import com.talent360.job.service.JobOfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Offers", description = "Job offer discovery, creation, updates, lifecycle, and recommendation")
public class JobOfferController {

    private final JobOfferService jobOfferService;

    @Operation(summary = "Search job offers", description = "Public endpoint with multi-criteria filtering and pagination")
    @GetMapping
    public ResponseEntity<PageResponse<JobOfferResponse>> searchJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ContractType contractType,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(jobOfferService.searchJobs(
                status, departmentId, location, contractType, workMode, experienceLevel, keyword, pageable
        ));
    }

    @Operation(summary = "Get job offer by ID", description = "Public endpoint to retrieve details of a specific job offer")
    @GetMapping("/{id}")
    public ResponseEntity<JobOfferResponse> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.getJobById(id));
    }

    @Operation(summary = "Create a job offer", description = "Recruiter only - starts in DRAFT status")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('RECRUITER')")
    @PostMapping
    public ResponseEntity<JobOfferResponse> createJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JobOfferRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobOfferService.createJob(request, userDetails.getUsername()));
    }

    @Operation(summary = "Update a job offer", description = "Recruiter owner or Admin only")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@ownershipSecurity.isJobOwner(#id, authentication) or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<JobOfferResponse> updateJob(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JobOfferRequest request
    ) {
        return ResponseEntity.ok(jobOfferService.updateJob(id, request, userDetails.getUsername()));
    }

    @Operation(summary = "Delete a job offer", description = "Recruiter owner or Admin only")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@ownershipSecurity.isJobOwner(#id, authentication) or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        jobOfferService.deleteJob(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Publish a job offer", description = "Changes status to OPEN and records a snapshot version")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@ownershipSecurity.isJobOwner(#id, authentication) or hasRole('ADMIN')")
    @PatchMapping("/{id}/publish")
    public ResponseEntity<JobOfferResponse> publishJob(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(jobOfferService.publishJob(id, userDetails.getUsername()));
    }

    @Operation(summary = "Close a job offer", description = "Changes status to CLOSED and records a snapshot version")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@ownershipSecurity.isJobOwner(#id, authentication) or hasRole('ADMIN')")
    @PatchMapping("/{id}/close")
    public ResponseEntity<JobOfferResponse> closeJob(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(jobOfferService.closeJob(id, userDetails.getUsername()));
    }

    @Operation(summary = "Get recommended jobs", description = "Candidate only - returns AI-matched job offers ranked by score")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CANDIDATE')")
    @GetMapping("/recommended")
    public ResponseEntity<PageResponse<JobOfferResponse>> getRecommendedJobs(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(jobOfferService.getRecommendedJobs(userDetails.getUsername(), pageable));
    }
}
