package com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
