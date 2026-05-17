package com.example.SmartHospital.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AdminDtos.AdminDashboardSummaryResponse;
import com.example.SmartHospital.dtos.AdminDtos.AdminReportSummaryResponse;
import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.service.admin.AdminReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminReportService adminReportService;

    @Operation(
        summary = "Get daily report",
        description = "Generate a daily report for a specific date showing appointment statistics and other metrics"
    )
    @GetMapping("/reports/daily")
    public ResponseEntity<ApiResponse<AdminReportSummaryResponse>> getDailyReport(@RequestParam LocalDate date) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Daily report generated", adminReportService.generateDaily(date)));
    }

    @Operation(
        summary = "Get monthly report",
        description = "Generate a monthly report for a specific year and month showing cumulative appointment statistics and metrics"
    )
    @GetMapping("/reports/monthly")
    public ResponseEntity<ApiResponse<AdminReportSummaryResponse>> getMonthlyReport(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Monthly report generated", adminReportService.generateMonthly(year, month)));
    }

    @Operation(
        summary = "Get yearly report",
        description = "Generate a yearly report for a specific year showing total appointment statistics and annual metrics"
    )
    @GetMapping("/reports/yearly")
    public ResponseEntity<ApiResponse<AdminReportSummaryResponse>> getYearlyReport(@RequestParam int year) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Yearly report generated", adminReportService.generateYearly(year)));
    }

    @Operation(
        summary = "Get dashboard summary",
        description = "Retrieve live admin dashboard statistics and recent issues"
    )
    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> getDashboardSummary() {
        return ResponseEntity.ok(new ApiResponse<>(200, "Dashboard summary generated", adminReportService.generateDashboardSummary()));
    }
}