package com.example.SmartHospital.model;
import java.time.LocalDateTime;
import java.util.List;

import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "prescription")
public class Prescription {
    @Id
    private String id;

    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generatePrescriptionId();
        }
        this.issueDate = LocalDateTime.now();
    }

    @Column(nullable=false)
    private LocalDateTime issueDate;
    private String notes;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @ManyToOne(optional=false)
    @JoinColumn(name="patient_id")
    private User patient;

    @ManyToOne
    @JoinColumn(name="doctor_id")
    private Doctor doctor;

    @ManyToMany
    @JoinTable(
        name = "prescription_medicine",
        joinColumns = @JoinColumn(name="prescription_id"),
        inverseJoinColumns = @JoinColumn(name="medicine_id")
    )
    private List<Medication> medicines;
}
