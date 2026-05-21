package com.example.SmartHospital.model;

import java.time.LocalDateTime;

import com.example.SmartHospital.enums.AppointmentStatus;
import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "appointment")
public class Appointment {
    @Id
    private String id;

    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generateAppointmentId();
        }
    }

    @Column(nullable=false)
    private LocalDateTime appointmentDateTime;
    @Column(nullable=false)
    private AppointmentStatus status;

    @Column(name = "appointment_type", length = 100)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String notes;
    private Integer rating;

    @Column(length = 1000)
    private String reviewComment;

    @Column(nullable=true)
    private String cancelReason;

    @ManyToOne(optional=false)
    @JoinColumn(name="patient_id")
    private Patient patient;

    @ManyToOne(optional=false)
    @JoinColumn(name="doctor_id")
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name="department_id")
    private Department department;

}
