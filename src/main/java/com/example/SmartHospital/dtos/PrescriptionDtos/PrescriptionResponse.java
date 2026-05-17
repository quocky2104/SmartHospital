package com.example.SmartHospital.dtos.PrescriptionDtos;

import java.time.LocalDateTime;
import java.util.List;

import com.example.SmartHospital.model.Prescription;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrescriptionResponse {
    private String prescriptionId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private LocalDateTime issueDate;
    private String notes;
    private List<String> medicationIds;
    private List<String> medicationNames;

    public PrescriptionResponse(Prescription prescription) {
        this.prescriptionId = prescription.getId();
        this.patientId = prescription.getPatient() == null ? null : prescription.getPatient().getId();
        this.patientName = prescription.getPatient() == null ? null : prescription.getPatient().getFullName();
        this.doctorId = prescription.getDoctor() == null ? null : prescription.getDoctor().getId();
        this.doctorName = prescription.getDoctor() == null ? null : prescription.getDoctor().getFullName();
        this.issueDate = prescription.getIssueDate();
        this.notes = prescription.getNotes();
        this.medicationIds = prescription.getMedicines() == null
            ? List.of()
            : prescription.getMedicines().stream().map(med -> med.getId()).toList();
        this.medicationNames = prescription.getMedicines() == null
            ? List.of()
            : prescription.getMedicines().stream().map(med -> med.getName()).toList();
    }
}