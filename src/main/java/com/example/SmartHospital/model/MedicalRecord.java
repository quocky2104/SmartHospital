package com.example.SmartHospital.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.SmartHospital.helper.CustomIdGenerator;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "medical_record")
public class MedicalRecord {
    @Id
    private String id;

    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generateMedicalRecordId();
        }
        this.createdAt = LocalDateTime.now();
    }

    private String treatmentNotes;
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean isDeleted = false;

    @ManyToOne(optional=true)
    @JoinColumn(name="doctor_id", nullable = true)
    private Doctor doctor;

    @ManyToOne(optional=false)
    @JoinColumn(name="patient_id")
    private Patient patient;

    @ElementCollection
    private List<String> attachments;

    @ElementCollection
    private List<String> diagnoses;
}
