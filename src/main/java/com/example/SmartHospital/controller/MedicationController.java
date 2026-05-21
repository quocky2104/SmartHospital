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
import com.example.SmartHospital.dtos.MedicationDtos.CreateReminderRequest;
import com.example.SmartHospital.dtos.MedicationDtos.MedicationRequest;
import com.example.SmartHospital.dtos.MedicationDtos.MedicationResponse;
import com.example.SmartHospital.dtos.MedicationDtos.ReminderDto;
import com.example.SmartHospital.service.medical.MedicationService;
import com.example.SmartHospital.service.medical.ReminderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medications")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MedicationController {
    private final MedicationService medicationService;
    private final ReminderService reminderService;

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
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @PostMapping
    public ResponseEntity<ApiResponse<MedicationResponse>> createMedication(@RequestBody MedicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(201, "Medication created", medicationService.createMedication(request)));
    }

    @Operation(summary = "Update medication", description = "Update a medication catalog entry. Usually admin-only in production")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @PutMapping("/{medicationId}")
    public ResponseEntity<ApiResponse<MedicationResponse>> updateMedication(
        @PathVariable String medicationId,
        @RequestBody MedicationRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Medication updated", medicationService.updateMedication(medicationId, request)));
    }

    @Operation(summary = "Delete medication", description = "Soft-delete a medication catalog entry")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @DeleteMapping("/{medicationId}/soft-delete")
    public ResponseEntity<ApiResponse<Void>> softDeleteMedication(@PathVariable String medicationId) {
        boolean deleted = medicationService.softDeleteMedication(medicationId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Medication not found", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Medication soft deleted", null));
    }

    @Operation(summary = "Hard delete medication", description = "Permanently remove a medication from the catalog and detach it from prescriptions")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @DeleteMapping("/{medicationId}/hard-delete")
    public ResponseEntity<ApiResponse<Void>> hardDeleteMedication(@PathVariable String medicationId) {
        boolean deleted = medicationService.hardDeleteMedication(medicationId);
        if (!deleted) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Medication not found", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "Medication hard deleted", null));
    }

    @Operation(summary = "Get reminders for medication", description = "Get all reminders associated with a specific medication")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @GetMapping("/{medId}/reminders")
    public ResponseEntity<List<ReminderDto>> getReminders(@PathVariable String medId) {
        List<ReminderDto> reminders = reminderService.findByMedicationId(medId);
        return ResponseEntity.ok(reminders);
    }

    @Operation(summary = "Get all reminders", description = "Get all reminders in the system")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @GetMapping("/reminders")
    public ResponseEntity<List<ReminderDto>> getAllReminders() {
        List<ReminderDto> reminders = reminderService.findAll();
        return ResponseEntity.ok(reminders);
    }

    @Operation(summary = "Create reminder for medication", description = "Create a new reminder associated with a specific medication")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @PostMapping("/{medId}/reminders")
    public ResponseEntity<ReminderDto> createReminder(@PathVariable String medId,
                                                      @RequestBody CreateReminderRequest req) {
        ReminderDto created = reminderService.create(medId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Delete reminder", description = "Soft-delete a reminder by its ID")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @DeleteMapping("/reminders/{reminderId}")
    public ResponseEntity<Void> deleteReminder(@PathVariable String reminderId) {
        reminderService.delete(reminderId);
        return ResponseEntity.noContent().build();
    }
}