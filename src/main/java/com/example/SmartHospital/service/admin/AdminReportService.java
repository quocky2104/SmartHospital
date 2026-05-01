package com.example.SmartHospital.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.AdminDtos.AdminReportSummaryResponse;
import com.example.SmartHospital.enums.AppointmentStatus;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.repository.AppointmentRepository;
import com.example.SmartHospital.repository.DoctorRepository;
import com.example.SmartHospital.repository.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminReportService {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AdminReportSummaryResponse generateDaily(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        return buildSummary("DAY", from, to);
    }

    public AdminReportSummaryResponse generateMonthly(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
        return buildSummary("MONTH", from, to);
    }

    public AdminReportSummaryResponse generateYearly(int year) {
        LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        return buildSummary("YEAR", from, to);
    }

    private AdminReportSummaryResponse buildSummary(String periodType, LocalDateTime from, LocalDateTime to) {
        long totalAppointments = appointmentRepository.countByAppointmentDateTimeBetween(from, to);
        long pendingAppointments = appointmentRepository.countByAppointmentDateTimeBetweenAndStatus(from, to, AppointmentStatus.PENDING);
        long scheduledAppointments = appointmentRepository.countByAppointmentDateTimeBetweenAndStatus(from, to, AppointmentStatus.SCHEDULED);
        long cancelledAppointments = appointmentRepository.countByAppointmentDateTimeBetweenAndStatus(from, to, AppointmentStatus.CANCELLED);
        long completedAppointments = appointmentRepository.countByAppointmentDateTimeBetweenAndStatus(from, to, AppointmentStatus.COMPLETED);

        long newPatients = patientRepository.countByCreatedAtBetween(from, to);
        long totalActivePatients = patientRepository.countByStatusNot(UserStatus.DELETED);

        long newDoctors = doctorRepository.countByCreatedAtBetween(from, to);
        long totalActiveDoctors = doctorRepository.countByStatusNot(UserStatus.DELETED);

        return new AdminReportSummaryResponse(
            periodType,
            from,
            to,
            totalAppointments,
            pendingAppointments,
            scheduledAppointments,
            cancelledAppointments,
            completedAppointments,
            newPatients,
            totalActivePatients,
            newDoctors,
            totalActiveDoctors
        );
    }
}