package com.example.SmartHospital.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.AdminDtos.AdminDashboardSummaryResponse;
import com.example.SmartHospital.dtos.AdminDtos.AdminReportSummaryResponse;
import com.example.SmartHospital.enums.AppointmentStatus;
import com.example.SmartHospital.enums.IssueStatus;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.Issue;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.AppointmentRepository;
import com.example.SmartHospital.repository.DepartmentRepository;
import com.example.SmartHospital.repository.IssueRepository;
import com.example.SmartHospital.repository.DoctorRepository;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminReportService {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

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

    public AdminDashboardSummaryResponse generateDashboardSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since30Days = now.minusDays(30);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime tomorrowStart = now.toLocalDate().plusDays(1).atStartOfDay();

        long totalDoctors = doctorRepository.countByStatusNot(UserStatus.DELETED);
        long totalPatients = patientRepository.countByStatusNot(UserStatus.DELETED);
        long totalDepartments = departmentRepository.countByIsDeletedFalse();

        long totalAppointmentsToday = appointmentRepository.countByAppointmentDateTimeBetween(todayStart, tomorrowStart);

        long openIssues = issueRepository.countByStatusAndIsDeletedFalse(IssueStatus.OPEN);
        long inProgressIssues = issueRepository.countByStatusAndIsDeletedFalse(IssueStatus.IN_PROGRESS);
        long resolvedIssues = issueRepository.countByStatusAndIsDeletedFalse(IssueStatus.RESOLVED);
        long closedIssues = issueRepository.countByStatusAndIsDeletedFalse(IssueStatus.CLOSED);

        long newDoctors30Days = doctorRepository.countByCreatedAtBetween(since30Days, now);
        long newPatients30Days = patientRepository.countByCreatedAtBetween(since30Days, now);
        long newIssues30Days = issueRepository.countByCreatedAtBetweenAndIsDeletedFalse(since30Days, now);

        List<AdminDashboardSummaryResponse.RecentIssueSummary> recentIssues = issueRepository
            .findTop5ByIsDeletedFalseOrderByCreatedAtDesc()
            .stream()
            .map(this::toRecentIssueSummary)
            .collect(Collectors.toList());

        return new AdminDashboardSummaryResponse(
            now,
            totalDoctors,
            totalPatients,
            totalDepartments,
            totalAppointmentsToday,
            openIssues,
            inProgressIssues,
            resolvedIssues,
            closedIssues,
            newDoctors30Days,
            newPatients30Days,
            newIssues30Days,
            recentIssues
        );
    }

    private AdminDashboardSummaryResponse.RecentIssueSummary toRecentIssueSummary(Issue issue) {
        User reporter = userRepository.findById(issue.getReporterId()).orElse(null);
        return new AdminDashboardSummaryResponse.RecentIssueSummary(
            issue.getId(),
            issue.getTitle(),
            reporter == null ? issue.getReporterId() : reporter.getFullName(),
            issue.getStatus() == null ? null : issue.getStatus().name(),
            issue.getCreatedAt()
        );
    }
}