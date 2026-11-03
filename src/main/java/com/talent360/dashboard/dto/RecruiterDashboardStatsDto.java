package com.talent360.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterDashboardStatsDto {
    private long totalJobs;
    private long activeJobs;
    private long totalApplications;
    private long totalCandidates;
    private long aiAnalyzedCount;
    private long topCandidatesCount;
    private List<JobApplicationStatDto> chartData;
    private List<RecentActivityDto> recentActivities;
}
