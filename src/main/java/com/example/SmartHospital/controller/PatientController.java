package com.example.SmartHospital.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.PaginatedResponse;
import com.example.SmartHospital.dtos.UserDtos.EditProfile.PatientEditProfileRequest;
import com.example.SmartHospital.dtos.UserDtos.PatientDTO;
import com.example.SmartHospital.service.patient.PatientManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class PatientController {
    private final PatientManagementService patientManagementService;

    @Operation(
        summary = "Get paginated list of patients",
        description = "Retrieve a paginated list of patients with optional search by name, email, phone, or identity number. Admin only"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getPatients")
    public ResponseEntity<ApiResponse<PaginatedResponse<PatientDTO>>> getPatients(
        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int pageNumber,
        @Parameter(description = "Page size (max 100)")
        @RequestParam(defaultValue = "10") int pageSize,
        @Parameter(description = "Search by name, email, phone, or identity number")
        @RequestParam(required = false) String search
    ) {
        try {
            PaginatedResponse<PatientDTO> response = patientManagementService.getPatients(pageNumber, pageSize, search);
            return ResponseEntity.ok(new ApiResponse<>(200, "Successfully retrieved patients", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to get patients", null));
        }
    }

    @Operation(
        summary = "Edit patient profile",
        description = "Update patient profile information including contact details, medical history, and avatar upload to MinIO"
    )
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping(value = "/patient/user-profile/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PatientDTO>> editPatientProfile(
        @RequestPart("request") PatientEditProfileRequest request,
        @AuthenticationPrincipal String userId,
        @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile,
        @RequestPart(value = "additionalFiles", required = false) java.util.List<MultipartFile> additionalFiles,
        @RequestParam(value = "removeAdditionalFiles", required = false) java.util.List<String> removeAdditionalFiles
    ) {
        PatientDTO response = patientManagementService.editPatientProfile(
            request,
            userId,
            avatarFile,
            additionalFiles,
            removeAdditionalFiles
        );
        return ResponseEntity.ok(new ApiResponse<>(200, "Successfully edited patient profile", response));
    }

    @Operation(
        summary = "View patient profile",
        description = "Retrieve the complete profile information of the authenticated patient"
    )
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/patient/user-profile/view")
    public ResponseEntity<ApiResponse<PatientDTO>> viewPatientProfile(@AuthenticationPrincipal String userId) {
        try {
            PatientDTO response = patientManagementService.getPatientById(userId);
            return ResponseEntity.ok(new ApiResponse<>(200, "Successfully retrieved patient profile", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to retrieve patient profile", null));
        }
    }

    @Operation(
        summary = "Soft delete patient",
        description = "Mark a patient as deleted without removing from database. Records remain for audit trail. Admin only"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/patient/{patientId}/soft-delete")
    public ResponseEntity<ApiResponse<Void>> softDeletePatient(@PathVariable String patientId) {
        boolean deleted = patientManagementService.softDeletePatient(patientId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Patient not found or already deleted", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Patient soft deleted successfully", null));
    }


    @Operation(
        summary = "Hard delete patient",
        description = "Permanently remove a patient and all associated records from database. Admin only. Cannot be undone"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/patient/{patientId}/hard-delete")
    public ResponseEntity<ApiResponse<Void>> hardDeletePatient(@PathVariable String patientId) {
        boolean deleted = patientManagementService.hardDeletePatient(patientId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Patient not found", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Patient hard deleted successfully", null));
    }
}