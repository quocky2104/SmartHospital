package com.example.SmartHospital.dtos.IssueDtos;

import java.time.LocalDateTime;

import com.example.SmartHospital.enums.IssueStatus;
import com.example.SmartHospital.model.Issue;

import lombok.Data;

@Data
public class IssueResponse {
    private String id;
    private String reporterId;
    private String reporterName;
    private String title;
    private String description;
    private IssueStatus status;
    private String adminResponse;
    private String assignedAdminId;
    private String assignedAdminName;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime updatedAt;

    public IssueResponse(Issue issue) {
        this.id = issue.getId();
        this.reporterId = issue.getReporterId();
        this.title = issue.getTitle();
        this.description = issue.getDescription();
        this.status = issue.getStatus();
        this.adminResponse = issue.getAdminResponse();
        this.createdAt = issue.getCreatedAt();
        this.resolvedAt = issue.getResolvedAt();
        this.updatedAt = issue.getUpdatedAt();
        
        if (issue.getAssignedAdmin() != null) {
            this.assignedAdminId = issue.getAssignedAdmin().getId();
            this.assignedAdminName = issue.getAssignedAdmin().getFullName();
        }
    }
}
