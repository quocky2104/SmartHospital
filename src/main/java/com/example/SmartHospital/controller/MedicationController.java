package com.example.SmartHospital.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.MedicationDtos.MedicationRequest;
import com.example.SmartHospital.dtos.MedicationDtos.MedicationResponse;
import com.example.SmartHospital.service.medical.MedicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medications")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MedicationController {
    private final MedicationService medicationService;

    @Operation(summary = "Get all medications", description = "Return the medication catalog")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicationResponse>>> getAllMedications() {
        return ResponseEntity.ok(new ApiResponse<>(200, "Medications fetched", medicationService.getAllMedications()));
    }

    @Operation(summary = "Get medication by ID", description = "Return one medication from the catalog")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @GetMapping("/{medicationId}")
    public ResponseEntity<ApiResponse<MedicationResponse>> getMedicationById(@PathVariable String medicationId) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Medication fetched", medicationService.getMedicationById(medicationId)));
    }

    @Operation(summary = "Create medication", description = "Create a medication catalog entry. Usually admin-only in production")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<MedicationResponse>> createMedication(@RequestBody MedicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(201, "Medication created", medicationService.createMedication(request)));
    }

    @Operation(summary = "Update medication", description = "Update a medication catalog entry. Usually admin-only in production")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{medicationId}")
    public ResponseEntity<ApiResponse<MedicationResponse>> updateMedication(
        @PathVariable String medicationId,
        @RequestBody MedicationRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Medication updated", medicationService.updateMedication(medicationId, request)));
    }

    @Operation(summary = "Delete medication", description = "Delete a medication catalog entry. Usually admin-only in production")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{medicationId}/soft-delete")
    public ResponseEntity<ApiResponse<Void>> softDeleteMedication(@PathVariable String medicationId) {
        boolean deleted = medicationService.softDeleteMedication(medicationId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Medication not found", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Medication soft deleted", null));
    }

    @Operation(summary = "Hard delete medication", description = "Permanently remove a medication from the catalog and detach it from prescriptions")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{medicationId}/hard-delete")
    public ResponseEntity<ApiResponse<Void>> hardDeleteMedication(@PathVariable String medicationId) {
        boolean deleted = medicationService.hardDeleteMedication(medicationId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Medication not found", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Medication hard deleted", null));
    }
}