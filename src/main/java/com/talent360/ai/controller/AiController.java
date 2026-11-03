package com.talent360.ai.controller;

import com.talent360.ai.cv.dto.CvAnalyzeRequest;
import com.talent360.ai.cv.dto.CvAnalysisResponse;
import com.talent360.ai.cv.service.CvAnalysisService;
import com.talent360.ai.jobdescription.dto.JdGenerateRequest;
import com.talent360.ai.jobdescription.dto.JdGeneratedDto;
import com.talent360.ai.jobdescription.service.JobDescriptionGeneratorService;
import com.talent360.ai.matching.dto.CandidateMatchResultDto;
import com.talent360.ai.matching.dto.CandidateMatchingRequest;
import com.talent360.ai.matching.dto.JobMatchResultDto;
import com.talent360.ai.matching.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Engine", description = "AI-powered features: CV extraction, candidate-job matching, and JD generation")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final CvAnalysisService cvAnalysisService;
    private final MatchingService matchingService;
    private final JobDescriptionGeneratorService jobDescriptionGeneratorService;

    @Operation(summary = "Analyze candidate CV", description = "Extracts structured skills, experience, and education from candidate document")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    @PostMapping("/cv/analyze")
    public ResponseEntity<CvAnalysisResponse> analyzeCv(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CvAnalyzeRequest request
    ) {
        return ResponseEntity.ok(cvAnalysisService.analyzeCv(request.getCandidateDocumentId(), userDetails.getUsername(), request.getForceReanalyze()));
    }

    @Operation(summary = "Match jobs for candidate", description = "Candidate only - returns ranked job offers with explanation breakdown")
    @PreAuthorize("hasRole('CANDIDATE')")
    @PostMapping("/matching/jobs")
    public ResponseEntity<List<JobMatchResultDto>> matchJobsForCandidate(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(matchingService.matchJobsForCandidate(userDetails.getUsername()));
    }

    @Operation(summary = "Match candidates for job offer", description = "Recruiter owner or Admin - returns ranked candidates with explanation breakdown")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @PostMapping("/matching/candidates")
    public ResponseEntity<List<CandidateMatchResultDto>> matchCandidatesForJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CandidateMatchingRequest request
    ) {
        return ResponseEntity.ok(matchingService.matchCandidatesForJob(request.getJobId(), userDetails.getUsername()));
    }

    @Operation(summary = "Generate job description", description = "Recruiter only - generates a structured JD draft (not persisted until confirmed)")
    @PreAuthorize("hasRole('RECRUITER')")
    @PostMapping("/job-description/generate")
    public ResponseEntity<JdGeneratedDto> generateJobDescription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JdGenerateRequest request
    ) {
        return ResponseEntity.ok(jobDescriptionGeneratorService.generateJobDescription(request, userDetails.getUsername()));
    }
}
