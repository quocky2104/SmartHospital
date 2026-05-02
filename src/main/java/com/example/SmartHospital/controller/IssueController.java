package com.example.SmartHospital.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.IssueDtos.CreateIssueRequest;
import com.example.SmartHospital.dtos.IssueDtos.IssueResponse;
import com.example.SmartHospital.dtos.IssueDtos.UpdateIssueStatusRequest;
import com.example.SmartHospital.enums.IssueStatus;
import com.example.SmartHospital.service.issue.IssueService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class IssueController {
    private final IssueService issueService;

    @Operation(
        summary = "Create a new issue report",
        description = "Submit an issue report from a doctor or patient to the admin"
    )
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<IssueResponse>> createIssue(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody CreateIssueRequest request
    ) {
        try {
            IssueResponse response = issueService.createIssue(userId, request);
            return ResponseEntity.ok(new ApiResponse<>(200, "Issue reported successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating issue", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to create issue", null));
        }
    }

    @Operation(
        summary = "Get my reported issues",
        description = "Retrieve all issues reported by the authenticated doctor or patient"
    )
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    @GetMapping("/my-issues")
    public ResponseEntity<ApiResponse<List<IssueResponse>>> getMyIssues(
        @AuthenticationPrincipal String userId
    ) {
        try {
            List<IssueResponse> issues = issueService.getIssuesByReporter(userId);
            return ResponseEntity.ok(new ApiResponse<>(200, "Issues retrieved successfully", issues));
        } catch (Exception e) {
            log.error("Error retrieving my issues", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to retrieve issues", null));
        }
    }

    @Operation(
        summary = "Get a specific issue",
        description = "Retrieve details of a specific issue by ID"
    )
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    @GetMapping("/{issueId}")
    public ResponseEntity<ApiResponse<IssueResponse>> getIssueById(
        @PathVariable String issueId
    ) {
        try {
            IssueResponse issue = issueService.getIssueById(issueId);
            return ResponseEntity.ok(new ApiResponse<>(200, "Issue retrieved successfully", issue));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error retrieving issue", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to retrieve issue", null));
        }
    }

    @Operation(
        summary = "Get all open issues",
        description = "Retrieve all open issues (admin only). Used for admin dashboard"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/open-issues")
    public ResponseEntity<ApiResponse<List<IssueResponse>>> getOpenIssues() {
        try {
            List<IssueResponse> issues = issueService.getAllOpenIssues();
            return ResponseEntity.ok(new ApiResponse<>(200, "Open issues retrieved successfully", issues));
        } catch (Exception e) {
            log.error("Error retrieving open issues", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to retrieve issues", null));
        }
    }

    @Operation(
        summary = "Get all issues",
        description = "Retrieve all issues (admin only)"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all-issues")
    public ResponseEntity<ApiResponse<List<IssueResponse>>> getAllIssues() {
        try {
            List<IssueResponse> issues = issueService.getAllIssues();
            return ResponseEntity.ok(new ApiResponse<>(200, "All issues retrieved successfully", issues));
        } catch (Exception e) {
            log.error("Error retrieving all issues", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to retrieve issues", null));
        }
    }

    @Operation(
        summary = "Get issues by status",
        description = "Retrieve issues filtered by status (admin only)"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/by-status")
    public ResponseEntity<ApiResponse<List<IssueResponse>>> getIssuesByStatus(
        @RequestParam IssueStatus status
    ) {
        try {
            List<IssueResponse> issues = issueService.getIssuesByStatus(status);
            return ResponseEntity.ok(new ApiResponse<>(200, "Issues retrieved successfully", issues));
        } catch (Exception e) {
            log.error("Error retrieving issues by status", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to retrieve issues", null));
        }
    }

    @Operation(
        summary = "Get issues assigned to me",
        description = "Retrieve all issues assigned to the authenticated admin"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/my-assigned-issues")
    public ResponseEntity<ApiResponse<List<IssueResponse>>> getAssignedIssues(
        @AuthenticationPrincipal String adminId
    ) {
        try {
            List<IssueResponse> issues = issueService.getIssuesAssignedToAdmin(adminId);
            return ResponseEntity.ok(new ApiResponse<>(200, "Assigned issues retrieved successfully", issues));
        } catch (Exception e) {
            log.error("Error retrieving assigned issues", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to retrieve issues", null));
        }
    }

    @Operation(
        summary = "Update issue status",
        description = "Update the status of an issue and optionally add an admin response (admin only)"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{issueId}/status")
    public ResponseEntity<ApiResponse<IssueResponse>> updateIssueStatus(
        @PathVariable String issueId,
        @AuthenticationPrincipal String adminId,
        @Valid @RequestBody UpdateIssueStatusRequest request
    ) {
        try {
            IssueResponse response = issueService.updateIssueStatus(issueId, adminId, request);
            return ResponseEntity.ok(new ApiResponse<>(200, "Issue status updated successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating issue status", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to update issue status", null));
        }
    }

    @Operation(
        summary = "Respond to an issue",
        description = "Add an admin response to an issue and set it to IN_PROGRESS (admin only)"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{issueId}/respond")
    public ResponseEntity<ApiResponse<IssueResponse>> respondToIssue(
        @PathVariable String issueId,
        @AuthenticationPrincipal String adminId,
        @RequestBody java.util.Map<String, String> body
    ) {
        try {
            String response = body.get("response");
            if (response == null || response.isBlank()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Response is required", null));
            }
            IssueResponse issueResponse = issueService.respondToIssue(issueId, adminId, response);
            return ResponseEntity.ok(new ApiResponse<>(200, "Response added successfully", issueResponse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error responding to issue", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to respond to issue", null));
        }
    }

    @Operation(
        summary = "Delete an issue",
        description = "Soft delete an issue (admin only)"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{issueId}")
    public ResponseEntity<ApiResponse<Void>> deleteIssue(
        @PathVariable String issueId
    ) {
        try {
            issueService.deleteIssue(issueId);
            return ResponseEntity.ok(new ApiResponse<>(200, "Issue deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error deleting issue", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to delete issue", null));
        }
    }
}
