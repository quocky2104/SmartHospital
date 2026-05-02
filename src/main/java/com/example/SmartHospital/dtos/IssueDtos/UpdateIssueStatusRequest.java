package com.example.SmartHospital.dtos.IssueDtos;

import com.example.SmartHospital.enums.IssueStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateIssueStatusRequest {
    @NotNull(message = "Status is required")
    private IssueStatus status;

    private String adminResponse;
}
