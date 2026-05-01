package com.example.SmartHospital.dtos.MedicalRecordDtos;

import java.util.List;

import lombok.Data;

@Data
public class MedicalRecordRequest {
    private String patientId;
    private String treatmentNotes;
    private List<String> attachments;
    private List<String> diagnoses;
}