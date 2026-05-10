package com.example.SmartHospital.dtos.ConsultationDtos;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class AnalyzeConsultationRequest {

    @NotBlank(message = "rawText is required")
    private String rawText;
}
