package com.example.SmartHospital.service.appointment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.AppointmentDtos.Request.AcceptAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.AppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.CancelAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Request.RescheduleAppointmentRequest;
import com.example.SmartHospital.dtos.AppointmentDtos.Response.Response.AppointmentResponse;
import com.example.SmartHospital.dtos.UserDtos.DoctorDTO;
import com.example.SmartHospital.enums.AppointmentStatus;
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

    public Page<AppointmentResponse> getPatientAppointments(
            String patientId,
            String search,
            Pageable pageable
    ) {
        return appointmentRepository
                .searchAppointmentsForPatient(patientId, search == null ? "" : search, pageable)
                .map(AppointmentResponse::new);
    }

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

        Appointment saved = appointmentRepository.save(appointment);
        notificationService.notifyDoctorAppointmentEvent(
            saved,
            "APPOINTMENT_CREATED",
            "New appointment request from " + patient.getFullName()
        );
        return new AppointmentResponse(saved);
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
        if (!saved.getDoctor().getId().equals(actorUserId)) {
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

        return new AppointmentResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse rescheduleAppointment(RescheduleAppointmentRequest request, String actorUserId) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
            .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Cancelled or completed appointment cannot be rescheduled");
        }

        validateAppointmentSlot(request.getNewAppointmentDate(), request.getNewAppointmentTime(), appointment.getDoctor());

        appointment.setAppointmentDateTime(LocalDateTime.of(request.getNewAppointmentDate(), request.getNewAppointmentTime()));
        if (request.getReason() != null && !request.getReason().isBlank()) {
            appointment.setNotes((appointment.getNotes() == null ? "" : appointment.getNotes() + " | ") + "Reschedule reason: " + request.getReason());
        }

        Appointment saved = appointmentRepository.save(appointment);
        if (!saved.getDoctor().getId().equals(actorUserId)) {
            notificationService.notifyDoctorAppointmentEvent(
                saved,
                "APPOINTMENT_RESCHEDULED",
                "Appointment rescheduled to " + saved.getAppointmentDateTime()
            );
        }
        return new AppointmentResponse(saved);
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
        availableDoctors = availableDoctors.stream()
            .filter(d -> d.getDepartment() != null)
            .filter(d -> !Boolean.TRUE.equals(d.getDepartment().getIsDeleted()))
            .filter(d -> departmentId == null || departmentId.isBlank() || departmentId.equals(d.getDepartment().getId()))
            .toList();

        return availableDoctors.stream()
                                .filter(d -> isWithinWorkingHours(d.getWorkingHours(), start, end)) // Filter by working hours
                                .map(d -> {
                                    DoctorDTO dto = new DoctorDTO();
                                    dto.setId(d.getId());
                                    dto.setFullName(d.getFullName());
                                    dto.setEmail(d.getEmail());
                                    dto.setSpecialization(d.getSpecialization());
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

}