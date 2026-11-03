package com.talent360.candidate.controller;

import com.talent360.candidate.dto.CandidateDocumentResponse;
import com.talent360.candidate.dto.CandidateResponse;
import com.talent360.candidate.dto.CandidateUpdateRequest;
import com.talent360.candidate.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidates", description = "Candidate profile management and CV document operations")
@SecurityRequirement(name = "bearerAuth")
public class CandidateController {

    private final CandidateService candidateService;

    @Operation(summary = "Get current candidate profile", description = "Candidate only - returns authenticated candidate's profile")
    @PreAuthorize("hasRole('CANDIDATE')")
    @GetMapping("/me")
    public ResponseEntity<CandidateResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(candidateService.getCandidateByEmail(userDetails.getUsername()));
    }

    @Operation(summary = "Get candidate profile by ID", description = "Candidate owner, Recruiter, or Admin only")
    @PreAuthorize("@ownershipSecurity.isCandidateOwner(#id, authentication) or hasAnyRole('RECRUITER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<CandidateResponse> getCandidateById(@PathVariable Long id) {
        return ResponseEntity.ok(candidateService.getCandidateById(id));
    }

    @Operation(summary = "Update candidate profile", description = "Only candidate owner or admin can update")
    @PreAuthorize("@ownershipSecurity.isCandidateOwner(#id, authentication) or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CandidateResponse> updateCandidate(
            @PathVariable Long id,
            @Valid @RequestBody CandidateUpdateRequest request
    ) {
        return ResponseEntity.ok(candidateService.updateCandidate(id, request));
    }

    @Operation(summary = "Upload current candidate CV document", description = "Multipart file upload for logged-in candidate")
    @PreAuthorize("hasRole('CANDIDATE')")
    @PostMapping(value = "/me/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateDocumentResponse> uploadMyCv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        CandidateResponse candidate = candidateService.getCandidateByEmail(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(candidateService.uploadCv(candidate.getId(), file));
    }

    @Operation(summary = "Download current candidate CV document", description = "Logged-in candidate only")
    @PreAuthorize("hasRole('CANDIDATE')")
    @GetMapping("/me/cv")
    public ResponseEntity<Resource> downloadMyCv(@AuthenticationPrincipal UserDetails userDetails) {
        CandidateResponse candidate = candidateService.getCandidateByEmail(userDetails.getUsername());
        Resource resource = candidateService.loadCvResource(candidate.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @Operation(summary = "Delete current candidate CV document", description = "Logged-in candidate only")
    @PreAuthorize("hasRole('CANDIDATE')")
    @DeleteMapping("/me/cv")
    public ResponseEntity<Void> deleteMyCv(@AuthenticationPrincipal UserDetails userDetails) {
        CandidateResponse candidate = candidateService.getCandidateByEmail(userDetails.getUsername());
        candidateService.deleteCv(candidate.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload candidate CV document (PDF/DOCX)", description = "Multipart file upload")
    @PreAuthorize("@ownershipSecurity.isCandidateOwner(#id, authentication) or hasRole('ADMIN')")
    @PostMapping(value = "/{id}/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateDocumentResponse> uploadCv(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(candidateService.uploadCv(id, file));
    }

    @Operation(summary = "Download candidate CV document", description = "Candidate owner, Recruiter, or Admin only")
    @PreAuthorize("@ownershipSecurity.isCandidateOwner(#id, authentication) or hasAnyRole('RECRUITER', 'ADMIN')")
    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> downloadCv(@PathVariable Long id) {
        Resource resource = candidateService.loadCvResource(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @Operation(summary = "Delete candidate CV", description = "Deletes the uploaded CV document")
    @PreAuthorize("@ownershipSecurity.isCandidateOwner(#id, authentication) or hasRole('ADMIN')")
    @DeleteMapping("/{id}/cv")
    public ResponseEntity<Void> deleteCv(@PathVariable Long id) {
        candidateService.deleteCv(id);
        return ResponseEntity.noContent().build();
    }
}