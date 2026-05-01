package com.example.SmartHospital.dtos.MedicationDtos;

import com.example.SmartHospital.model.Medication;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MedicationResponse {
    private String medicationId;
    private String name;
    private String dosage;
    private String duration;
    private String frequency;

    public MedicationResponse(Medication medication) {
        this.medicationId = medication.getId();
        this.name = medication.getName();
        this.dosage = medication.getDosage();
        this.duration = medication.getDuration();
        this.frequency = medication.getFrequency();
    }
}