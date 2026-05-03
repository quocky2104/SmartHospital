package com.example.SmartHospital.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name="patient")
@Data
@EqualsAndHashCode(callSuper = false)
public class Patient extends User{
    @OneToMany(mappedBy="patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalRecord> medicalRecords;

    @Column(unique=true)
    private String insuranceNumber;
    
    private String insuranceProvider;

    private String insuranceId;

    private String bloodType;

    @ElementCollection
    @CollectionTable(name = "patient_emergency_contacts", joinColumns = @JoinColumn(name = "patient_id"))
    private List<EmergencyContact> emergencyContacts;

    @ElementCollection
    @CollectionTable(name = "patient_additional_files", joinColumns = @JoinColumn(name = "patient_id"))
    @Column(name = "file_path")
    private List<String> additionalFiles; // optional
}
