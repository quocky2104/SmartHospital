package com.example.SmartHospital.dtos.AdminDtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardSummaryResponse {
    private LocalDateTime generatedAt;
    private long totalDoctors;
    private long totalPatients;
    private long totalDepartments;
    private long totalAppointmentsToday;
    private long openIssues;
    private long inProgressIssues;
    private long resolvedIssues;
    private long closedIssues;
    private long newDoctors30Days;
    private long newPatients30Days;
    private long newIssues30Days;
    private List<RecentIssueSummary> recentIssues;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentIssueSummary {
        private String id;
        private String title;
        private String reporterName;
        private String status;
        private LocalDateTime createdAt;
    }
}