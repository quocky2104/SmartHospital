package com.example.SmartHospital.service.appointment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.SmartHospital.config.exceptions.ConflictException;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.AcceptAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.AppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.AppointmentReviewRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.CancelAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.RescheduleAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Response.Response.AppointmentResponse;
import com.example.SmartHospital.dtos.UserDtos.DoctorDTO;
import com.example.SmartHospital.enums.AppointmentStatus;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.Appointment;
import com.example.SmartHospital.model.Doctor;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.repository.AppointmentRepository;
import com.example.SmartHospital.repository.DoctorRepository;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;

        @Transactional(readOnly = true)
        public Page<AppointmentResponse> getPatientAppointments(
            String patientId,
            String search,
            Pageable pageable
    ) {
        return appointmentRepository
                .searchAppointmentsForPatient(patientId, search == null ? "" : search, pageable)
                .map(AppointmentResponse::new);
    }

        @Transactional(readOnly = true)
        public Page<AppointmentResponse> getDoctorAppointments(
            String doctorId,
            String search,
            Pageable pageable
    ) {
        return appointmentRepository
                .searchAppointmentsForDoctor(doctorId, search == null ? "" : search, pageable)
                .map(AppointmentResponse::new);
    }

    public AppointmentResponse createAppointment(AppointmentRequest request, String patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        validateAppointmentSlot(request.getAppointmentDate(), request.getAppointmentTime(), doctor);
        validatePatientTimeslotConflict(
            patient.getId(),
            request.getAppointmentDate(),
            request.getAppointmentTime(),
            null
        );

        Appointment appointment = new Appointment();
        appointment.setAppointmentDateTime(
            LocalDateTime.of(
                request.getAppointmentDate(),
                request.getAppointmentTime()
            )
        );
        appointment.setNotes(request.getNotes());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setDepartment(doctor.getDepartment());

        appointment.setType(normalizeAppointmentType(request.getType()));

        Appointment saved = appointmentRepository.save(appointment);
        notificationService.notifyDoctorAppointmentEvent(
            saved,
            "APPOINTMENT_CREATED",
            "New appointment request from " + patient.getFullName()
        );
        return new AppointmentResponse(saved);
    }

    private String normalizeAppointmentType(String type) {
        if (type == null || type.isBlank()) {
            return "Check-up";
        }

        String trimmed = type.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("open requests to review:") || trimmed.contains("\"main_symptoms\"")) {
            return "Check-up";
        }

        return trimmed;
    }

    public AppointmentResponse cancelAppointment(CancelAppointmentRequest request, String actorUserId) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED
            && appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new RuntimeException("Only SCHEDULED or PENDING appointments can be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(request.getCancelReason());

        Appointment saved = appointmentRepository.save(appointment);
        boolean cancelledByDoctor = saved.getDoctor().getId().equals(actorUserId);
        if (cancelledByDoctor) {
            notificationService.notifyPatientAppointmentEvent(
                saved,
                "APPOINTMENT_CANCELLED",
                "Your appointment was cancelled: " + (request.getCancelReason() == null ? "No reason provided" : request.getCancelReason())
            );
        } else {
            notificationService.notifyDoctorAppointmentEvent(
                saved,
                "APPOINTMENT_CANCELLED",
                "Appointment cancelled: " + (request.getCancelReason() == null ? "No reason provided" : request.getCancelReason())
            );
        }

        return new AppointmentResponse(saved);
    }

    public AppointmentResponse acceptAppointment(AcceptAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new RuntimeException("Only PENDING appointments can be accepted");
        }

        appointment.setStatus(AppointmentStatus.SCHEDULED);
        Appointment saved = appointmentRepository.save(appointment);
        notificationService.notifyPatientAppointmentEvent(
            saved,
            "APPOINTMENT_ACCEPTED",
            "Your appointment was accepted and scheduled for " + saved.getAppointmentDateTime()
        );

        return new AppointmentResponse(saved);
    }

    public AppointmentResponse completeAppointment(String appointmentId, String doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Access denied");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("Cancelled appointment cannot be marked as done");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment saved = appointmentRepository.save(appointment);
        notificationService.notifyPatientAppointmentEvent(
            saved,
            "APPOINTMENT_COMPLETED",
            "Your appointment has been marked as completed by the doctor."
        );
        return new AppointmentResponse(saved);
    }

    public AppointmentResponse rateAppointment(String appointmentId, String patientId, AppointmentReviewRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new RuntimeException("Access denied");
        }

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Only completed appointments can be rated");
        }

        appointment.setRating(request.getRating());
        appointment.setReviewComment(request.getComment() == null ? null : request.getComment().trim());
        Appointment saved = appointmentRepository.save(appointment);
        return new AppointmentResponse(saved);
    }

    public AppointmentResponse rescheduleAppointment(RescheduleAppointmentRequest request, String actorUserId) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
            .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Cancelled or completed appointment cannot be rescheduled");
        }

        validateAppointmentSlot(request.getNewAppointmentDate(), request.getNewAppointmentTime(), appointment.getDoctor());
        validatePatientTimeslotConflict(
            appointment.getPatient().getId(),
            request.getNewAppointmentDate(),
            request.getNewAppointmentTime(),
            appointment.getId()
        );

        appointment.setAppointmentDateTime(LocalDateTime.of(request.getNewAppointmentDate(), request.getNewAppointmentTime()));
        if (request.getReason() != null && !request.getReason().isBlank()) {
            appointment.setNotes((appointment.getNotes() == null ? "" : appointment.getNotes() + " | ") + "Reschedule reason: " + request.getReason());
        }

        Appointment saved = appointmentRepository.save(appointment);
        boolean rescheduledByDoctor = saved.getDoctor().getId().equals(actorUserId);
        if (rescheduledByDoctor) {
            notificationService.notifyPatientAppointmentEvent(
                saved,
                "APPOINTMENT_RESCHEDULED",
                "Your appointment was rescheduled to " + saved.getAppointmentDateTime()
            );
        } else {
            notificationService.notifyDoctorAppointmentEvent(
                saved,
                "APPOINTMENT_RESCHEDULED",
                "Appointment rescheduled to " + saved.getAppointmentDateTime()
            );
        }
        return new AppointmentResponse(saved);
    }

    // Scheduled task to auto-cancel expired pending appointments every minute (configurable)
    @Scheduled(fixedDelayString = "${app.appointments.auto-cancel-check-ms:60000}")
    @Transactional
    public void autoCancelExpiredPendingAppointments() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> expiredPendingAppointments = appointmentRepository
            .findByStatusAndAppointmentDateTimeBeforeOrderByAppointmentDateTimeAsc(AppointmentStatus.PENDING, now);

        for (Appointment appointment : expiredPendingAppointments) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancelReason("Automatically cancelled because the appointment time passed before acceptance.");
            Appointment saved = appointmentRepository.save(appointment);
            notificationService.notifyPatientAppointmentEvent(
                saved,
                "APPOINTMENT_AUTO_CANCELLED",
                "Your appointment expired and was automatically cancelled because no action was taken before the scheduled time."
            );
            notificationService.notifyDoctorAppointmentEvent(
                saved,
                "APPOINTMENT_AUTO_CANCELLED",
                "Appointment automatically cancelled because it passed without acceptance."
            );
        }
    }

        public List<DoctorDTO> findAvailableDoctors(
            LocalDate date,
            LocalTime time,
            String departmentId
        ) {
        validateTimeslotInput(date, time);

        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end   = start.plusMinutes(30);

        List<String> busyDoctorIds =
            appointmentRepository.findBusyDoctorIdsByStatuses(
                start,
                end,
                List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.PENDING)
            );

        List<Doctor> availableDoctors =
            appointmentRepository.findAvailableDoctors(
                busyDoctorIds.isEmpty() ? null : busyDoctorIds
            );

        // If departmentId provided, filter doctors to that department
        // Exclude Emergency department doctors from patient bookings
        availableDoctors = availableDoctors.stream()
            .filter(d -> d.getDepartment() != null)
            .filter(d -> !Boolean.TRUE.equals(d.getDepartment().getIsDeleted()))
            .filter(d -> departmentId == null || departmentId.isBlank() || departmentId.equals(d.getDepartment().getId()))
            .filter(d -> !"Emergency".equalsIgnoreCase(d.getDepartment().getName()))
            .toList();

        return availableDoctors.stream()
                                .sorted((left, right) -> {
                                    String leftName = left.getFullName() == null ? "" : left.getFullName();
                                    String rightName = right.getFullName() == null ? "" : right.getFullName();
                                    int nameCompare = leftName.compareToIgnoreCase(rightName);
                                    if (nameCompare != 0) {
                                        return nameCompare;
                                    }
                                    String leftId = left.getId() == null ? "" : left.getId();
                                    String rightId = right.getId() == null ? "" : right.getId();
                                    return leftId.compareToIgnoreCase(rightId);
                                })
                                .filter(d -> isWithinWorkingHours(d.getWorkingHours(), start, end)) // Filter by working hours
                                .map(d -> {
                                    DoctorDTO dto = new DoctorDTO();
                                    dto.setId(d.getId());
                                    dto.setFullName(d.getFullName());
                                    dto.setDepartmentName(d.getDepartment() == null ? null : d.getDepartment().getName());
                                    dto.setEmail(d.getEmail());
                                    dto.setWorkingHours(d.getWorkingHours());
                                    dto.setAvailabilityStatus(d.getAvailabilityStatus());
                                    dto.setDepartmentId(d.getDepartment() == null ? null : d.getDepartment().getId());
                                    return dto;
                                })
                                .toList();
    }

    /**
     * Get available timeslots for a specific doctor on a given date.
     * Returns 30-minute slots from working hours, excluding booked times.
     */
    public List<String> findAvailableTimeslots(String doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new RuntimeException("Doctor not found"));

        String workingHours = doctor.getWorkingHours();
        if (workingHours == null || workingHours.isBlank()) {
            return List.of(); // No working hours defined
        }

        List<String> slots = new java.util.ArrayList<>();
        
        // Parse working hours (e.g., "07:00-20:00")
        String[] ranges = workingHours.split(",");
        for (String range : ranges) {
            range = range.trim();
            String[] parts = range.split("-");
            if (parts.length != 2) continue;

            try {
                LocalTime startHour = LocalTime.parse(parts[0].trim());
                LocalTime endHour = LocalTime.parse(parts[1].trim());

                // Generate 30-minute slots
                LocalTime current = startHour;
                while (current.isBefore(endHour)) {
                    LocalDateTime slotDateTime = LocalDateTime.of(date, current);
                    LocalDateTime slotEnd = slotDateTime.plusMinutes(30);

                    // Check if slot is booked
                    List<Appointment> bookings = appointmentRepository.findByDoctorAndAppointmentDateTimeBetweenAndStatus(
                        doctor,
                        slotDateTime,
                        slotEnd,
                        List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.PENDING)
                    );

                    if (bookings.isEmpty()) {
                        slots.add(String.format("%02d:%02d", current.getHour(), current.getMinute()));
                    }

                    current = current.plusMinutes(30);
                }
            } catch (Exception e) {
                // Skip invalid time formats
            }
        }

        return slots;
    }

    public List<DoctorDTO> getBookingDoctors(String departmentId) {
        return doctorRepository.findAll().stream()
            .filter(d -> d.getStatus() != UserStatus.DELETED)
            .filter(d -> d.getDepartment() != null)
            .filter(d -> !Boolean.TRUE.equals(d.getDepartment().getIsDeleted()))
            .filter(d -> departmentId == null || departmentId.isBlank() || departmentId.equals(d.getDepartment().getId()))
            .filter(d -> !"Emergency".equalsIgnoreCase(d.getDepartment().getName()))
            .sorted((left, right) -> {
                String leftName = left.getFullName() == null ? "" : left.getFullName();
                String rightName = right.getFullName() == null ? "" : right.getFullName();
                int nameCompare = leftName.compareToIgnoreCase(rightName);
                if (nameCompare != 0) {
                    return nameCompare;
                }
                String leftId = left.getId() == null ? "" : left.getId();
                String rightId = right.getId() == null ? "" : right.getId();
                return leftId.compareToIgnoreCase(rightId);
            })
            .map(d -> {
                DoctorDTO dto = new DoctorDTO();
                dto.setId(d.getId());
                dto.setFullName(d.getFullName());
                dto.setDepartmentName(d.getDepartment() == null ? null : d.getDepartment().getName());
                dto.setEmail(d.getEmail());
                dto.setWorkingHours(d.getWorkingHours());
                dto.setAvailabilityStatus(d.getAvailabilityStatus());
                dto.setDepartmentId(d.getDepartment() == null ? null : d.getDepartment().getId());
                return dto;
            })
            .toList();
    }

    // Utility method to check if a time is within working hours
    // For example, "08:00-12:00,14:00-18:00"
    private boolean isWithinWorkingHours(String workingHours, LocalDateTime startTime, LocalDateTime endTime) {
        if (workingHours == null || workingHours.isBlank()) return false;

        String[] ranges = workingHours.split(","); // Split by comma
        for (String range : ranges) {
            String[] parts = range.trim().split("-");
            if (parts.length != 2) { // Invalid range
                continue; 
            }

            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());

            if (!startTime.toLocalTime().isBefore(start) && !endTime.toLocalTime().isAfter(end)) {
                return true;
            }
        }

        return false;
    }

    private void validateAppointmentSlot(LocalDate date, LocalTime time, Doctor doctor) {
        validateTimeslotInput(date, time);

        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end = start.plusMinutes(30);

        if (!isWithinWorkingHours(doctor.getWorkingHours(), start, end)) {
            throw new RuntimeException("Selected timeslot is outside doctor's working hours");
        }

        List<String> busyDoctorIds = appointmentRepository.findBusyDoctorIdsByStatuses(
            start,
            end,
            List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.PENDING)
        );
        if (busyDoctorIds.contains(doctor.getId())) {
            throw new RuntimeException("Selected doctor is not available at this timeslot");
        }
    }

    private void validateTimeslotInput(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw new RuntimeException("Appointment date and time are required");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new RuntimeException("Appointment date cannot be in the past");
        }
        if (date.isEqual(LocalDate.now()) && time.isBefore(LocalTime.now())) {
            throw new IllegalArgumentException("Appointment time cannot be in the past");
        }
        if (!(time.getMinute() == 0 || time.getMinute() == 30) || time.getSecond() != 0 || time.getNano() != 0) {
            throw new IllegalArgumentException("Timeslot must be on 30-minute boundaries using HH:mm:ss format");
        }
    }

    private void validatePatientTimeslotConflict(String patientId, LocalDate date, LocalTime time, String excludeAppointmentId) {
        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end = start.plusMinutes(30);
        List<AppointmentStatus> blockingStatuses = List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.PENDING);

        boolean hasConflict = excludeAppointmentId == null
            ? appointmentRepository.existsPatientConflictAtTimeslot(patientId, start, end, blockingStatuses)
            : appointmentRepository.existsPatientConflictAtTimeslotExcludingAppointment(
                patientId,
                excludeAppointmentId,
                start,
                end,
                blockingStatuses
            );

        if (hasConflict) {
            throw new ConflictException("You already have an appointment at this timeslot. Please choose a different time.");
        }
    }

}