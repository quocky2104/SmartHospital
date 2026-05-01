package com.example.SmartHospital.dtos.MedicationDtos;

import lombok.Data;

@Data
public class MedicationRequest {
    private String name;
    private String dosage;
    private String duration;
    private String frequency;
}