package com.example.SmartHospital.dtos.MedicalRecordDtos;

import java.time.LocalDateTime;
import java.util.List;

import com.example.SmartHospital.model.MedicalRecord;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MedicalRecordResponse {
    private String recordId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private LocalDateTime createdAt;
    private String treatmentNotes;
    private List<String> attachments;
    private List<String> diagnoses;

    public MedicalRecordResponse(MedicalRecord medicalRecord) {
        this.recordId = medicalRecord.getId();
        this.patientId = medicalRecord.getPatient() == null ? null : medicalRecord.getPatient().getId();
        this.patientName = medicalRecord.getPatient() == null ? null : medicalRecord.getPatient().getFullName();
        this.doctorId = medicalRecord.getDoctor() == null ? null : medicalRecord.getDoctor().getId();
        this.doctorName = medicalRecord.getDoctor() == null ? null : medicalRecord.getDoctor().getFullName();
        this.createdAt = medicalRecord.getCreatedAt();
        this.treatmentNotes = medicalRecord.getTreatmentNotes();
        this.attachments = medicalRecord.getAttachments();
        this.diagnoses = medicalRecord.getDiagnoses();
    }
}