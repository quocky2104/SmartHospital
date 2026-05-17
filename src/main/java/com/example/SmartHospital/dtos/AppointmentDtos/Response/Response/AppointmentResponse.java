package com.example.SmartHospital.dtos.AppointmentDtos.Response.Response;

import com.example.SmartHospital.model.Appointment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppointmentResponse {
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String appointmentDate;
    private String appointmentTime;
    private String status;
    private String notes;
    private Integer rating;
    private String reviewComment;
    private String cancelReason;

    // Constructor to map from Appointment entity (assuming such an entity exists)
    public AppointmentResponse(Appointment appointment) {
        this.appointmentId = appointment.getId();
        this.patientId = appointment.getPatient() == null ? null : appointment.getPatient().getId();
        this.patientName = appointment.getPatient() == null ? null : appointment.getPatient().getFullName();
        this.doctorId = appointment.getDoctor() == null ? null : appointment.getDoctor().getId();
        this.doctorName = appointment.getDoctor() == null ? null : appointment.getDoctor().getFullName();
        this.appointmentDate = appointment.getAppointmentDateTime() == null ? null : appointment.getAppointmentDateTime().toLocalDate().toString();
        this.appointmentTime = appointment.getAppointmentDateTime() == null ? null : appointment.getAppointmentDateTime().toLocalTime().toString();
        this.status = appointment.getStatus() == null ? null : appointment.getStatus().name();
        this.notes = appointment.getNotes();
        this.rating = appointment.getRating();
        this.reviewComment = appointment.getReviewComment();
        this.cancelReason = appointment.getCancelReason();
    }
}
