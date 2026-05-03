package com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmergencyContactRequest {
    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String relationship;
}
