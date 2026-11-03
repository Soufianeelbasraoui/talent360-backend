package com.talent360.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long publishedJobs;
    private long totalApplications;
    private long totalDepartments;
    private List<Map<String, Object>> growthData;
}

