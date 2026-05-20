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
    private String recordType;
    private String recordTitle;
    private String summary;
    private LocalDateTime createdAt;
    private String treatmentNotes;
    private String labName;
    private String resultValue;
    private String resultUnit;
    private String referenceRange;
    private String resultStatus;
    private List<String> attachments;
    private List<String> attachmentUrls;
    private List<String> diagnoses;

    public MedicalRecordResponse(MedicalRecord medicalRecord) {
        this(medicalRecord, null);
    }

    public MedicalRecordResponse(MedicalRecord medicalRecord, List<String> attachmentUrls) {
        this.recordId = medicalRecord.getId();
        this.patientId = medicalRecord.getPatient() == null ? null : medicalRecord.getPatient().getId();
        this.patientName = medicalRecord.getPatient() == null ? null : medicalRecord.getPatient().getFullName();
        this.doctorId = medicalRecord.getDoctor() == null ? null : medicalRecord.getDoctor().getId();
        this.doctorName = medicalRecord.getDoctor() == null ? null : medicalRecord.getDoctor().getFullName();
        this.recordType = medicalRecord.getRecordType();
        this.recordTitle = medicalRecord.getRecordTitle();
        this.summary = medicalRecord.getSummary();
        this.createdAt = medicalRecord.getCreatedAt();
        this.treatmentNotes = medicalRecord.getTreatmentNotes();
        this.labName = medicalRecord.getLabName();
        this.resultValue = medicalRecord.getResultValue();
        this.resultUnit = medicalRecord.getResultUnit();
        this.referenceRange = medicalRecord.getReferenceRange();
        this.resultStatus = medicalRecord.getResultStatus();
        this.attachments = medicalRecord.getAttachments();
        this.attachmentUrls = attachmentUrls;
        this.diagnoses = medicalRecord.getDiagnoses();
    }
}