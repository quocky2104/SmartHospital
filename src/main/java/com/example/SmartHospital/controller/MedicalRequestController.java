package com.example.SmartHospital.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.MedicalRequestDtos.CreateMedicalRequestDto;
import com.example.SmartHospital.dtos.MedicalRequestDtos.MedicalRequestResponse;
import com.example.SmartHospital.service.medical.PatientMedicalRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MedicalRequestController {

    private final PatientMedicalRequestService patientMedicalRequestService;

    @Operation(summary = "Create a medical / intake request (patient)")
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<MedicalRequestResponse>> create(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody CreateMedicalRequestDto body
    ) {
        MedicalRequestResponse created = patientMedicalRequestService.create(userId, body);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(201, "Request created", created));
    }

    @Operation(summary = "List requests (patient: own; doctor: all)")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicalRequestResponse>>> list(Authentication authentication) {
        List<MedicalRequestResponse> list = patientMedicalRequestService.list(authentication);
        return ResponseEntity.ok(new ApiResponse<>(200, "Requests retrieved", list));
    }

    @Operation(summary = "Get one request by id")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicalRequestResponse>> getById(
        @PathVariable String id,
        Authentication authentication
    ) {
        MedicalRequestResponse one = patientMedicalRequestService.getById(id, authentication);
        return ResponseEntity.ok(new ApiResponse<>(200, "Request retrieved", one));
    }

    @Operation(summary = "Close a medical request")
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<MedicalRequestResponse>> close(
        @PathVariable String id,
        Authentication authentication
    ) {
        MedicalRequestResponse updated = patientMedicalRequestService.closeRequest(id, authentication);
        return ResponseEntity.ok(new ApiResponse<>(200, "Request closed", updated));
    }

    @Operation(summary = "Reopen a medical request")
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/{id}/reopen")
    public ResponseEntity<ApiResponse<MedicalRequestResponse>> reopen(
        @PathVariable String id,
        Authentication authentication
    ) {
        MedicalRequestResponse updated = patientMedicalRequestService.reopenRequest(id, authentication);
        return ResponseEntity.ok(new ApiResponse<>(200, "Request reopened", updated));
    }
}
