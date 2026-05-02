package com.example.SmartHospital.service.issue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.IssueDtos.CreateIssueRequest;
import com.example.SmartHospital.dtos.IssueDtos.IssueResponse;
import com.example.SmartHospital.dtos.IssueDtos.UpdateIssueStatusRequest;
import com.example.SmartHospital.enums.IssueStatus;
import com.example.SmartHospital.model.Admin;
import com.example.SmartHospital.model.Issue;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.IssueRepository;
import com.example.SmartHospital.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueService {
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    /**
     * Create a new issue report from a user (doctor or patient)
     */
    public IssueResponse createIssue(String reporterId, CreateIssueRequest request) {
        // Verify user exists
        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Issue issue = new Issue();
        issue.setReporterId(reporterId);
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setStatus(IssueStatus.OPEN);

        Issue savedIssue = issueRepository.save(issue);
        IssueResponse response = new IssueResponse(savedIssue);
        response.setReporterName(reporter.getFullName());
        return response;
    }

    /**
     * Get all issues reported by a specific user
     */
    public List<IssueResponse> getIssuesByReporter(String reporterId) {
        List<Issue> issues = issueRepository.findByReporterIdAndIsDeletedFalse(reporterId);
        return enrichIssuesWithReporterNames(issues);
    }

    /**
     * Get all open issues (for admin dashboard)
     */
    public List<IssueResponse> getAllOpenIssues() {
        List<Issue> issues = issueRepository.findByStatusOrderByCreatedAtDesc(IssueStatus.OPEN);
        return enrichIssuesWithReporterNames(issues);
    }

    /**
     * Get all issues
     */
    public List<IssueResponse> getAllIssues() {
        List<Issue> issues = issueRepository.findAllActiveIssues();
        return enrichIssuesWithReporterNames(issues);
    }

    /**
     * Get issues by status
     */
    public List<IssueResponse> getIssuesByStatus(IssueStatus status) {
        List<Issue> issues = issueRepository.findByStatusOrderByCreatedAtDesc(status);
        return enrichIssuesWithReporterNames(issues);
    }

    /**
     * Get issues assigned to a specific admin
     */
    public List<IssueResponse> getIssuesAssignedToAdmin(String adminId) {
        Admin admin = (Admin) userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        
        List<Issue> issues = issueRepository.findByAssignedAdminAndIsDeletedFalse(admin);
        return enrichIssuesWithReporterNames(issues);
    }

    /**
     * Get a specific issue by ID
     */
    public IssueResponse getIssueById(String issueId) {
        Issue issue = issueRepository.findByIdAndIsDeletedFalse(issueId)
            .orElseThrow(() -> new IllegalArgumentException("Issue not found"));
        
        IssueResponse response = new IssueResponse(issue);
        // Enrich with reporter name
        User reporter = userRepository.findById(issue.getReporterId()).orElse(null);
        if (reporter != null) {
            response.setReporterName(reporter.getFullName());
        }
        return response;
    }

    /**
     * Update issue status (admin only)
     */
    public IssueResponse updateIssueStatus(String issueId, String adminId, UpdateIssueStatusRequest request) {
        Issue issue = issueRepository.findByIdAndIsDeletedFalse(issueId)
            .orElseThrow(() -> new IllegalArgumentException("Issue not found"));

        Admin admin = (Admin) userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        issue.setStatus(request.getStatus());
        issue.setAssignedAdmin(admin);
        issue.setUpdatedAt(LocalDateTime.now());

        if (request.getAdminResponse() != null) {
            issue.setAdminResponse(request.getAdminResponse());
        }

        if (request.getStatus() == IssueStatus.RESOLVED || request.getStatus() == IssueStatus.CLOSED) {
            issue.setResolvedAt(LocalDateTime.now());
        }

        Issue updatedIssue = issueRepository.save(issue);
        IssueResponse response = new IssueResponse(updatedIssue);
        
        // Enrich with reporter name
        User reporter = userRepository.findById(issue.getReporterId()).orElse(null);
        if (reporter != null) {
            response.setReporterName(reporter.getFullName());
        }
        return response;
    }

    /**
     * Add response to an issue (admin only)
     */
    public IssueResponse respondToIssue(String issueId, String adminId, String response) {
        Issue issue = issueRepository.findByIdAndIsDeletedFalse(issueId)
            .orElseThrow(() -> new IllegalArgumentException("Issue not found"));

        Admin admin = (Admin) userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        issue.setAdminResponse(response);
        issue.setAssignedAdmin(admin);
        issue.setStatus(IssueStatus.IN_PROGRESS);
        issue.setUpdatedAt(LocalDateTime.now());

        Issue updatedIssue = issueRepository.save(issue);
        IssueResponse issueResponse = new IssueResponse(updatedIssue);
        
        // Enrich with reporter name
        User reporter = userRepository.findById(issue.getReporterId()).orElse(null);
        if (reporter != null) {
            issueResponse.setReporterName(reporter.getFullName());
        }
        return issueResponse;
    }

    /**
     * Soft delete an issue
     */
    public void deleteIssue(String issueId) {
        Issue issue = issueRepository.findByIdAndIsDeletedFalse(issueId)
            .orElseThrow(() -> new IllegalArgumentException("Issue not found"));
        
        issue.setIsDeleted(true);
        issueRepository.save(issue);
    }

    // Helper method to enrich issues with reporter names
    private List<IssueResponse> enrichIssuesWithReporterNames(List<Issue> issues) {
        return issues.stream().map(issue -> {
            IssueResponse response = new IssueResponse(issue);
            User reporter = userRepository.findById(issue.getReporterId()).orElse(null);
            if (reporter != null) {
                response.setReporterName(reporter.getFullName());
            }
            return response;
        }).collect(Collectors.toList());
    }
}
