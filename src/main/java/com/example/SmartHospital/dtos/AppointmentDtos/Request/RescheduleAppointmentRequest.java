package com.example.SmartHospital.dtos.AppointmentDtos.Request;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RescheduleAppointmentRequest {
    private String appointmentId;
    @Schema(description = "New appointment date", example = "2026-04-26", format = "date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate newAppointmentDate;
    @Schema(description = "New appointment time in 24h format", example = "10:30:00", format = "time")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime newAppointmentTime;
    private String reason;
}