package com.example.SmartHospital.dtos.MedicalRecordDtos;

import java.util.List;

import lombok.Data;

@Data
public class MedicalRecordRequest {
    private String patientId;
    private String recordType;
    private String recordTitle;
    private String summary;
    private String treatmentNotes;
    private String labName;
    private String resultValue;
    private String resultUnit;
    private String referenceRange;
    private String resultStatus;
    private List<String> attachments;
    private List<String> diagnoses;
}