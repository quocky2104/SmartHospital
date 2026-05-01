package com.example.SmartHospital.model;

import java.util.List;
import java.time.LocalDateTime;

import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "medication")
public class Medication {
    @Id
    private String id;
    
    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generateMedicationId();
        }
    }

    @Column(nullable=false)
    private String name;
    private String dosage;
    private String duration;
    private String frequency;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @ManyToMany(mappedBy = "medicines")
    private List<Prescription> prescriptions;
}
