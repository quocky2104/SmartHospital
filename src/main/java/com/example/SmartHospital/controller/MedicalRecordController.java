package com.example.SmartHospital.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.MedicalRecordDtos.MedicalRecordRequest;
import com.example.SmartHospital.dtos.MedicalRecordDtos.MedicalRecordResponse;
import com.example.SmartHospital.service.medical.MedicalRecordService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medical-records")
@PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
@RequiredArgsConstructor
public class MedicalRecordController {
    private final MedicalRecordService medicalRecordService;

    @Operation(summary = "Get a medical record by ID", description = "Patients can access their own records, doctors can access records of their patients")
    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> getMedicalRecord(@AuthenticationPrincipal String userId, @PathVariable String recordId) {
        MedicalRecordResponse response = medicalRecordService.getMedicalRecord(userId, recordId);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(403, "Access denied", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Medical record fetched", response));
    }

    @Operation(summary = "Get my medical records", description = "Patients get their records and doctors get records assigned to them")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MedicalRecordResponse>>> getMyMedicalRecords(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Medical records fetched", medicalRecordService.getMyMedicalRecords(userId)));
    }

    @Operation(summary = "Create a medical record", description = "Doctor in charge creates a new medical record for a patient")
    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> createMedicalRecord(
        @AuthenticationPrincipal String doctorId,
        @RequestBody MedicalRecordRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(201, "Medical record created", medicalRecordService.createMedicalRecord(doctorId, request)));
    }

    @Operation(summary = "Edit a medical record", description = "Only the doctor in charge can edit the medical record")
    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/{recordId}")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> updateMedicalRecord(
        @AuthenticationPrincipal String doctorId,
        @PathVariable String recordId,
        @RequestBody MedicalRecordRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Medical record updated", medicalRecordService.updateMedicalRecord(doctorId, recordId, request)));
    }

    @Operation(summary = "Soft delete a medical record", description = "Mark a medical record as deleted without removing it from the database")
    @PreAuthorize("hasRole('DOCTOR')")
    @DeleteMapping("/{recordId}/soft-delete")
    public ResponseEntity<ApiResponse<Void>> softDeleteMedicalRecord(
        @AuthenticationPrincipal String doctorId,
        @PathVariable String recordId
    ) {
        boolean deleted = medicalRecordService.softDeleteMedicalRecord(doctorId, recordId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Medical record not found or access denied", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Medical record soft deleted", null));
    }

    @Operation(summary = "Hard delete a medical record", description = "Permanently remove a medical record from the database")
    @PreAuthorize("hasRole('DOCTOR')")
    @DeleteMapping("/{recordId}/hard-delete")
    public ResponseEntity<ApiResponse<Void>> hardDeleteMedicalRecord(
        @AuthenticationPrincipal String doctorId,
        @PathVariable String recordId
    ) {
        boolean deleted = medicalRecordService.hardDeleteMedicalRecord(doctorId, recordId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Medical record not found or access denied", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Medical record hard deleted", null));
    }
}
