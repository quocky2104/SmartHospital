package com.example.SmartHospital.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AppointmentDtos.Request.AcceptAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.AppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.AppointmentReviewRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.CancelAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.RescheduleAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Response.Response.AppointmentResponse;
import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.UserDtos.DoctorDTO;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.service.appointment.AppointmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/appointment")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final PatientRepository patientRepository;

    @Operation(
        summary = "Get patient's appointments",
        description = "Retrieve a paginated list of appointments for the authenticated patient with optional search and sorting"
    )
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/patient/getAppointments")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getPatientAppointments(
        @AuthenticationPrincipal String userId,
        @RequestParam(required = false, defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "appointmentDateTime") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.fromString(direction), sortBy)
        );

        Page<AppointmentResponse> result =
                appointmentService.getPatientAppointments(userId, search, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointments retrieved successfully", result)
        );
    }

    @Operation(
        summary = "Get doctor's appointments",
        description = "Retrieve a paginated list of appointments for the authenticated doctor with optional search and sorting"
    )
    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/getAppointments")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getDoctorAppointments(
        @AuthenticationPrincipal String userId,
        @RequestParam(required = false, defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "appointmentDateTime") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.fromString(direction), sortBy)
        );

        Page<AppointmentResponse> result =
            appointmentService.getDoctorAppointments(userId, search, pageable);

        return ResponseEntity.ok(
            new ApiResponse<>(200, "Appointments retrieved successfully", result)
        );
    }

    @Operation(
        summary = "Create new appointment",
        description = "Create a new appointment by selecting a doctor and an available timeslot. Patient specifies the appointment date, time, and optional notes"
    )
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/createAppointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
        @RequestBody AppointmentRequest request,
        @AuthenticationPrincipal String userId
    ) {
        if (userId == null || !patientRepository.existsById(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(403, "Only patients can create appointments", null));
        }

        try {
            AppointmentResponse response = appointmentService.createAppointment(request, userId);
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "Appointment created successfully", response)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Failed to create appointment: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Get doctors available for booking",
        description = "Retrieve a patient-safe list of active doctors, optionally filtered by department"
    )
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/booking-doctors")
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getBookingDoctors(
        @RequestParam(required = false) String departmentId
    ) {
        List<DoctorDTO> doctors = appointmentService.getBookingDoctors(departmentId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Doctors retrieved successfully", doctors));
    }

    @Operation(
        summary = "Cancel an appointment",
        description = "Cancel an existing appointment. Both patient and doctor can cancel with an optional cancellation reason"
    )
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @PostMapping("/cancelAppointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
        @RequestBody CancelAppointmentRequest request,
        @AuthenticationPrincipal String userId
    ) {
        try {
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "Appointment cancelled successfully", appointmentService.cancelAppointment(request, userId))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Failed to cancel appointment: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Reschedule an appointment",
        description = "Change the date and/or time of an existing appointment. Both patient and doctor can reschedule with an optional reason"
    )
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @PostMapping("/rescheduleAppointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rescheduleAppointment(
        @RequestBody RescheduleAppointmentRequest request,
        @AuthenticationPrincipal String userId
    ) {
        try {
            return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointment rescheduled successfully", appointmentService.rescheduleAppointment(request, userId))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "Failed to reschedule appointment: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Cancel appointment by id (path)",
        description = "Cancel appointment using path id and optional reason in body { reason: '...' }"
    )
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @PostMapping("/cancel/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelById(
        @PathVariable("id") String id,
        @RequestBody(required = false) Map<String, String> body,
        @AuthenticationPrincipal String userId
    ) {
        try {
            CancelAppointmentRequest req = new CancelAppointmentRequest();
            req.setAppointmentId(id);
            if (body != null) {
                req.setCancelReason(body.getOrDefault("reason", body.get("cancelReason")));
            }
            return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointment cancelled successfully", appointmentService.cancelAppointment(req, userId))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "Failed to cancel appointment: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Reschedule appointment by id (path)",
        description = "Reschedule appointment using path id and body { appointmentDate: 'yyyy-MM-dd', appointmentTime: 'HH:mm', reason: '...'}"
    )
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @PostMapping("/reschedule/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rescheduleById(
        @PathVariable("id") String id,
        @RequestBody Map<String, String> body,
        @AuthenticationPrincipal String userId
    ) {
        try {
            RescheduleAppointmentRequest req = new RescheduleAppointmentRequest();
            req.setAppointmentId(id);
            if (body != null) {
                String dateStr = body.getOrDefault("appointmentDate", body.get("newAppointmentDate"));
                String timeStr = body.getOrDefault("appointmentTime", body.get("newAppointmentTime"));
                if (dateStr != null) req.setNewAppointmentDate(LocalDate.parse(dateStr));
                if (timeStr != null) {
                    // accept HH:mm or HH:mm:ss
                    if (timeStr.length() == 5) timeStr = timeStr + ":00";
                    req.setNewAppointmentTime(LocalTime.parse(timeStr));
                }
                req.setReason(body.get("reason"));
            }
            return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointment rescheduled successfully", appointmentService.rescheduleAppointment(req, userId))
            );
        } catch (DateTimeParseException dtpe) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, "Invalid date/time format: " + dtpe.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "Failed to reschedule appointment: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Accept appointment by id (path)",
        description = "Doctor accepts a pending appointment via path id"
    )
    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/accept/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> acceptById(@PathVariable("id") String id) {
        try {
            AcceptAppointmentRequest req = new AcceptAppointmentRequest();
            req.setAppointmentId(id);
            return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointment accepted successfully", appointmentService.acceptAppointment(req))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "Failed to accept appointment: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Decline (cancel) appointment by id (path)",
        description = "Doctor declines an appointment; treated as cancel with optional reason"
    )
    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/decline/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> declineById(
        @PathVariable("id") String id,
        @RequestBody(required = false) Map<String, String> body,
        @AuthenticationPrincipal String userId
    ) {
        try {
            CancelAppointmentRequest req = new CancelAppointmentRequest();
            req.setAppointmentId(id);
            if (body != null) {
                req.setCancelReason(body.getOrDefault("reason", body.get("cancelReason")));
            }
            return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointment declined successfully", appointmentService.cancelAppointment(req, userId))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "Failed to decline appointment: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Accept a pending appointment",
        description = "Doctor accepts a pending appointment request to confirm their availability"
    )
    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/acceptAppointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> acceptAppointment(@RequestBody AcceptAppointmentRequest request) {
        try {
            return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointment accepted successfully", appointmentService.acceptAppointment(request))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Failed to accept appointment: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Mark appointment as completed",
        description = "Doctor marks an accepted/scheduled appointment as done"
    )
    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/complete/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> completeAppointment(
        @PathVariable("id") String id,
        @AuthenticationPrincipal String userId
    ) {
        try {
            return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointment completed successfully", appointmentService.completeAppointment(id, userId))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "Failed to complete appointment: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Rate a completed appointment",
        description = "Patient can rate and leave a comment for a completed appointment"
    )
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/rate/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rateAppointment(
        @PathVariable("id") String id,
        @RequestBody @jakarta.validation.Valid AppointmentReviewRequest request,
        @AuthenticationPrincipal String userId
    ) {
        try {
            return ResponseEntity.ok(
                new ApiResponse<>(200, "Appointment rated successfully", appointmentService.rateAppointment(id, userId, request))
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "Failed to rate appointment: " + e.getMessage(), null));
        }
    }

    @Operation(
        summary = "Get available doctors for a timeslot",
        description = "Retrieve list of available doctors for a specific date and time. Filters by working hours and existing appointments"
    )
    @GetMapping("/available-doctors") 
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')") 
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getAvailableDoctors( 
        @Schema(description = "Date of the appointment", example = "2026-04-26", format = "date")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam LocalDate date,
        @Schema(description = "Time slot in 24h format", example = "10:00:00", format = "time")
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        @RequestParam LocalTime time,
        @RequestParam(required = false) String departmentId ) {
            List<DoctorDTO> doctors = appointmentService.findAvailableDoctors(date, time, departmentId); 
            return ResponseEntity.ok(new ApiResponse<>(200, "Available doctors retrieved", doctors)); 
    }

    @Operation(
        summary = "Get available timeslots for a doctor",
        description = "Retrieve list of available time slots for a specific doctor on a given date"
    )
    @GetMapping("/available-timeslots")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableTimeslots(
        @RequestParam String doctorId,
        @Schema(description = "Date of the appointment", example = "2026-04-26", format = "date")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam LocalDate date) {
        List<String> slots = appointmentService.findAvailableTimeslots(doctorId, date);
        return ResponseEntity.ok(new ApiResponse<>(200, "Available timeslots retrieved", slots));
    }
}
