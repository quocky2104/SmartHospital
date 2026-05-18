package com.example.SmartHospital.dtos.AuthDtos.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private boolean twoFactorRequired;
    private String accessToken;
    private String verifyToken;
}
