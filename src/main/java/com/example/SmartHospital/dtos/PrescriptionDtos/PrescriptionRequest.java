package com.example.SmartHospital.dtos.PrescriptionDtos;

import java.util.List;

import lombok.Data;

@Data
public class PrescriptionRequest {
    private String patientId;
    private List<String> medicationIds;
    private String notes;
}