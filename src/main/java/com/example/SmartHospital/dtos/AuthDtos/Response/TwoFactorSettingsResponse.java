package com.example.SmartHospital.dtos.AuthDtos.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TwoFactorSettingsResponse {
    private boolean twoFactorEnabled;
}
