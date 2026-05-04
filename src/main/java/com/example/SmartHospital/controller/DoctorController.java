package com.example.SmartHospital.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.PaginatedResponse;
import com.example.SmartHospital.dtos.UserDtos.DoctorCreateRequest;
import com.example.SmartHospital.dtos.UserDtos.DoctorDTO;
import com.example.SmartHospital.dtos.UserDtos.EditProfile.DoctorEditProfileRequest;
import com.example.SmartHospital.service.doctor.DoctorManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {
    private final DoctorManagementService doctorManagementService;

    @Operation(
        summary = "Get paginated list of doctors",
        description = "Retrieve a paginated list of doctors with optional search by name, email, phone, or identity number. Admin only"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getDoctors")
    public ResponseEntity<ApiResponse<PaginatedResponse<DoctorDTO>>> getDoctors(
        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int pageNumber,
        @Parameter(description = "Page size (max 100)")
        @RequestParam(defaultValue = "10") int pageSize,
        @Parameter(description = "Search by name, email, phone, or identity number")
        @RequestParam(required = false) String search,
        @Parameter(description = "Filter by department ID")
        @RequestParam(required = false) String departmentId
    ) {
        try {
            PaginatedResponse<DoctorDTO> response = doctorManagementService.getDoctors(pageNumber, pageSize, search, departmentId);
            return ResponseEntity.ok(new ApiResponse<>(200, "Successfully retrieved doctors", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to get doctors", null));
        }
    }

    @Operation(
        summary = "Edit doctor profile",
        description = "Update doctor profile information including department working hours, and avatar upload to MinIO"
    )
    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/doctor/user-profile/edit")
    public ResponseEntity<ApiResponse<DoctorDTO>> editDoctorProfile(
        @RequestBody DoctorEditProfileRequest request,
        @AuthenticationPrincipal String userId,
        @RequestParam(required = false) MultipartFile avatarFile
    ) {
        try {
            DoctorDTO response = doctorManagementService.editDoctorProfile(request, userId, avatarFile);
            return ResponseEntity.ok(new ApiResponse<>(200, "Successfully edited doctor profile", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to edit doctor profile", null));
        }
    }

    @Operation(
        summary = "View doctor profile",
        description = "Retrieve the complete profile information of the authenticated doctor"
    )
    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/user-profile/view")
    public ResponseEntity<ApiResponse<DoctorDTO>> viewDoctorProfile(@AuthenticationPrincipal String userId) {
        try {
            DoctorDTO response = doctorManagementService.getDoctorById(userId);
            return ResponseEntity.ok(new ApiResponse<>(200, "Successfully retrieved doctor profile", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to retrieve doctor profile", null));
        }
    }

    @Operation(
        summary = "Soft delete doctor",
        description = "Mark a doctor as deleted without removing from database. Records remain for audit trail. Admin only"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/doctor/{doctorId}/soft-delete")
    public ResponseEntity<ApiResponse<Void>> softDeleteDoctor(@PathVariable String doctorId) {
        boolean deleted = doctorManagementService.softDeleteDoctor(doctorId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Doctor not found or already deleted", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Doctor soft deleted successfully", null));
    }

    @Operation(
        summary = "Hard delete doctor",
        description = "Permanently remove a doctor and all associated records from database. Admin only. Cannot be undone"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/doctor/{doctorId}/hard-delete")
    public ResponseEntity<ApiResponse<Void>> hardDeleteDoctor(@PathVariable String doctorId) {
        boolean deleted = doctorManagementService.hardDeleteDoctor(doctorId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Doctor not found", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Doctor hard deleted successfully", null));
    }

    @Operation(
        summary = "Create a new doctor",
        description = "Create a new doctor account with details via form. Admin only"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/doctor/create")
    public ResponseEntity<ApiResponse<DoctorDTO>> createDoctor(@Valid @RequestBody DoctorCreateRequest request) {
        try {
            DoctorDTO response = doctorManagementService.createDoctor(request);
            return ResponseEntity.status(201).body(new ApiResponse<>(201, "Doctor created successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse<>(500, "Failed to create doctor", null));
        }
    }

    @Operation(
        summary = "Admin edit doctor",
        description = "Update doctor profile information as an admin"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/doctors/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorDTO>> editDoctorByAdmin(
        @PathVariable String doctorId,
        @Valid @RequestBody DoctorEditProfileRequest request
    ) {
        try {
            DoctorDTO response = doctorManagementService.editDoctorByAdmin(doctorId, request);
            if (response == null) {
                return ResponseEntity.status(404).body(new ApiResponse<>(404, "Doctor not found", null));
            }
            return ResponseEntity.ok(new ApiResponse<>(200, "Doctor updated successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse<>(500, "Failed to update doctor", null));
        }
    }
}