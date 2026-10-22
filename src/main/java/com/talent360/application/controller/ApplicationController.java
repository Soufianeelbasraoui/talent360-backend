package com.talent360.application.controller;

import com.talent360.application.dto.ApplicationRequest;
import com.talent360.application.dto.ApplicationResponse;
import com.talent360.application.dto.ApplicationStatusUpdateRequest;
import com.talent360.application.service.ApplicationService;
import com.talent360.common.dto.PageResponse;
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
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Candidate job applications lifecycle and management")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "Submit an application", description = "Candidate only - applies to an open job offer")
    @PreAuthorize("hasRole('CANDIDATE')")
    @PostMapping("/api/applications")
    public ResponseEntity<ApplicationResponse> apply(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApplicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.apply(request, userDetails.getUsername()));
    }

    @Operation(summary = "List candidate applications", description = "Candidate only - returns current candidate's applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    @GetMapping("/api/applications/me")
    public ResponseEntity<PageResponse<ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(applicationService.getMyApplications(userDetails.getUsername(), pageable));
    }

    @Operation(summary = "List applications for a job", description = "Recruiter owner only - returns applications sortable by matchingScore")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @GetMapping("/api/jobs/{jobId}/applications")
    public ResponseEntity<PageResponse<ApplicationResponse>> getJobApplications(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "matchingScore", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(applicationService.getJobApplications(jobId, userDetails.getUsername(), pageable));
    }

    @Operation(summary = "Update application status", description = "Recruiter owner or Admin - moves application through hiring workflow")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @PatchMapping("/api/applications/{id}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApplicationStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, request.getStatus(), userDetails.getUsername()));
    }
}
