package com.example.SmartHospital.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.ConsultationDtos.AnalyzeConsultationRequest;
import com.example.SmartHospital.dtos.ConsultationDtos.ConsultationExtractResponse;
import com.example.SmartHospital.service.consultation.GeminiConsultationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/consultations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ConsultationAnalysisController {

    private final GeminiConsultationService geminiConsultationService;

    @Operation(
        summary = "Analyze medical consultation text with Gemini",
        description = "Structured symptom extraction for intake (no diagnosis). Uses server-side Gemini API key."
    )
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<ConsultationExtractResponse>> analyze(
        @Valid @RequestBody AnalyzeConsultationRequest request
    ) {
        ConsultationExtractResponse data = geminiConsultationService.analyze(request.getRawText());
        return ResponseEntity.ok(new ApiResponse<>(200, "Consultation analyzed", data));
    }
}
