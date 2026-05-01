package com.example.SmartHospital.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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

    @ElementCollection // For storing a list of emergency contact
    //JPA stores them in a separate collection table tied to the owning entity.
    @Column(name="emergency_contact")
    private List<String> emergencyContacts;
}
