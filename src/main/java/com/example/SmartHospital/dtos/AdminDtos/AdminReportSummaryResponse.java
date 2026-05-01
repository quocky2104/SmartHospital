package com.example.SmartHospital.dtos.AdminDtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminReportSummaryResponse {
    private String periodType;
    private LocalDateTime from;
    private LocalDateTime to;
    private long totalAppointments;
    private long pendingAppointments;
    private long scheduledAppointments;
    private long cancelledAppointments;
    private long completedAppointments;
    private long newPatients;
    private long totalActivePatients;
    private long newDoctors;
    private long totalActiveDoctors;
}