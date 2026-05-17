package com.example.SmartHospital.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.PrescriptionDtos.PrescriptionRequest;
import com.example.SmartHospital.dtos.PrescriptionDtos.PrescriptionResponse;
import com.example.SmartHospital.service.medical.PrescriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/prescriptions")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class PrescriptionController {
    private final PrescriptionService prescriptionService;

    @Operation(summary = "Get my prescriptions", description = "Patients can list their own prescriptions")
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getMyPrescriptions(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Prescriptions fetched", prescriptionService.getMyPrescriptions(userId)));
    }

    @Operation(summary = "Get prescriptions created by me", description = "Doctors can list prescriptions they created")
    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/my-doctor")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getMyPrescriptionsAsDoctor(@AuthenticationPrincipal String doctorId) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Prescriptions fetched", prescriptionService.getMyPrescriptionsByDoctor(doctorId)));
    }

    @Operation(summary = "Get prescription by ID", description = "Patients can open their own prescription and doctors can open prescriptions they created")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @GetMapping("/{prescriptionId}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getPrescriptionById(
        @AuthenticationPrincipal String userId,
        @PathVariable String prescriptionId
    ) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(200, "Prescription fetched", prescriptionService.getPrescriptionById(userId, prescriptionId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(403, e.getMessage(), null));
        }
    }

    @Operation(summary = "Create prescription", description = "Doctors create a prescription for a patient and assign medications by IDs")
    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping
    public ResponseEntity<ApiResponse<PrescriptionResponse>> createPrescription(
        @AuthenticationPrincipal String doctorId,
        @RequestBody PrescriptionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(201, "Prescription created", prescriptionService.createPrescription(doctorId, request)));
    }

    @Operation(summary = "Edit prescription", description = "Doctors can update an existing prescription they created, including notes, patient, and medication list")
    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/{prescriptionId}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> updatePrescription(
        @AuthenticationPrincipal String doctorId,
        @PathVariable String prescriptionId,
        @RequestBody PrescriptionRequest request
    ) {
        return ResponseEntity.ok(
            new ApiResponse<>(200, "Prescription updated", prescriptionService.updatePrescription(doctorId, prescriptionId, request))
        );
    }

    @Operation(summary = "Soft delete prescription", description = "Doctor in charge can mark a prescription as deleted without removing it from the database")
    @PreAuthorize("hasRole('DOCTOR')")
    @DeleteMapping("/{prescriptionId}/soft-delete")
    public ResponseEntity<ApiResponse<Void>> softDeletePrescription(
        @AuthenticationPrincipal String doctorId,
        @PathVariable String prescriptionId
    ) {
        boolean deleted = prescriptionService.softDeletePrescription(doctorId, prescriptionId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Prescription not found or access denied", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Prescription soft deleted", null));
    }

    @Operation(summary = "Hard delete prescription", description = "Doctor in charge can permanently remove a prescription")
    @PreAuthorize("hasRole('DOCTOR')")
    @DeleteMapping("/{prescriptionId}/hard-delete")
    public ResponseEntity<ApiResponse<Void>> hardDeletePrescription(
        @AuthenticationPrincipal String doctorId,
        @PathVariable String prescriptionId
    ) {
        boolean deleted = prescriptionService.hardDeletePrescription(doctorId, prescriptionId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Prescription not found or access denied", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Prescription hard deleted", null));
    }
}