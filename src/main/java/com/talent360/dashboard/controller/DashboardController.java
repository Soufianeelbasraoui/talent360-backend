package com.talent360.dashboard.controller;

import com.talent360.dashboard.dto.AdminDashboardStatsDto;
import com.talent360.dashboard.dto.RecruiterDashboardStatsDto;
import com.talent360.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Analytics", description = "Real-time statistics for Recruiter and Admin dashboards")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get recruiter dashboard stats", description = "Recruiter and Admin only - returns real counts, chart data, and recent activity")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @GetMapping("/recruiter")
    public ResponseEntity<RecruiterDashboardStatsDto> getRecruiterStats(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(dashboardService.getRecruiterStats(userDetails.getUsername()));
    }

    @Operation(summary = "Get admin dashboard stats", description = "Admin only - returns system metrics, user counts, and growth trends")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<AdminDashboardStatsDto> getAdminStats() {
        return ResponseEntity.ok(dashboardService.getAdminStats());
    }
}

