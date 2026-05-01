package com.example.SmartHospital.dtos.AppointmentDtos.Request;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AppointmentRequest {
    private String doctorId;
    @Schema(description = "Date of the appointment", example = "2026-04-26", format = "date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;
    @Schema(description = "Time slot in 24h format", example = "10:00:00", format = "time")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime appointmentTime;
    private String notes;
}
