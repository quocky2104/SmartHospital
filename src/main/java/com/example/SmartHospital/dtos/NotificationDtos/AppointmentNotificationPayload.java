package com.example.SmartHospital.dtos.NotificationDtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppointmentNotificationPayload {
    private String eventType;
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private LocalDateTime appointmentDateTime;
    private String message;
}