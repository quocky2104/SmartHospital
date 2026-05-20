package com.example.SmartHospital.service.medical;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.MedicalRecordDtos.MedicalRecordRequest;
import com.example.SmartHospital.dtos.MedicalRecordDtos.MedicalRecordResponse;
import com.example.SmartHospital.enums.RoleType;
import com.example.SmartHospital.model.Doctor;
import com.example.SmartHospital.model.MedicalRecord;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.DoctorRepository;
import com.example.SmartHospital.repository.MedicalRecordRepository;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.repository.UserRepository;
import com.example.SmartHospital.service.storage.MinioStorageService;
import com.example.SmartHospital.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final MinioStorageService minioStorageService;
    private final NotificationService notificationService;

    public MedicalRecordResponse getMedicalRecord(String userId, String recordId) {
        User currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        MedicalRecord medicalRecord = medicalRecordRepository.findByIdAndIsDeletedFalse(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Medical record not found"));

        boolean patientOwner = currentUser.getRole() == RoleType.PATIENT
            && medicalRecord.getPatient() != null
            && medicalRecord.getPatient().getId().equals(userId);

        boolean doctorOwner = currentUser.getRole() == RoleType.DOCTOR
            && medicalRecord.getDoctor() != null
            && medicalRecord.getDoctor().getId().equals(userId);

        if (!patientOwner && !doctorOwner) {
            return null;
        }

        return toResponse(medicalRecord);
    }

    public List<MedicalRecordResponse> getPatientMedicalRecords(String userId, String patientId) {
        User currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (currentUser.getRole() == RoleType.PATIENT && !currentUser.getId().equals(patientId)) {
            throw new IllegalArgumentException("Access denied");
        }

        return medicalRecordRepository.findAllByPatient_IdAndIsDeletedFalseOrderByCreatedAtDesc(patientId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public MedicalRecordResponse createMedicalRecord(String doctorId, MedicalRecordRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setDoctor(doctor);
        medicalRecord.setPatient(patient);
        medicalRecord.setRecordType(normalizeRecordType(request.getRecordType()));
        medicalRecord.setRecordTitle(resolveRecordTitle(request));
        medicalRecord.setSummary(resolveSummary(request));
        medicalRecord.setTreatmentNotes(request.getTreatmentNotes());
        medicalRecord.setLabName(request.getLabName());
        medicalRecord.setResultValue(request.getResultValue());
        medicalRecord.setResultUnit(request.getResultUnit());
        medicalRecord.setReferenceRange(request.getReferenceRange());
        medicalRecord.setResultStatus(request.getResultStatus());
        medicalRecord.setAttachments(request.getAttachments());
        medicalRecord.setDiagnoses(request.getDiagnoses());
        medicalRecord.setIsDeleted(false);

        MedicalRecord saved = medicalRecordRepository.save(medicalRecord);
        // notify patient that a new medical record was added
        try {
            notificationService.notifyUserEvent(
                patient.getId(),
                patient.getEmail(),
                patient.getFullName(),
                "medical_record.created",
                "New medical record",
                String.format("Dr. %s added a new medical record: %s", doctor.getFullName(), saved.getRecordTitle()),
                saved.getId()
            );
        } catch (Exception ex) {
            // don't fail the request if notification fails
        }
        return toResponse(saved);
    }

    public MedicalRecordResponse updateMedicalRecord(String doctorId, String recordId, MedicalRecordRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        MedicalRecord medicalRecord = medicalRecordRepository.findByIdAndIsDeletedFalse(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Medical record not found"));

        if (medicalRecord.getDoctor() == null || !medicalRecord.getDoctor().getId().equals(doctor.getId())) {
            throw new IllegalArgumentException("Only the doctor in charge can edit this medical record");
        }

        if (request.getRecordType() != null) {
            medicalRecord.setRecordType(normalizeRecordType(request.getRecordType()));
        }
        if (request.getRecordTitle() != null) {
            medicalRecord.setRecordTitle(request.getRecordTitle());
        }
        if (request.getSummary() != null) {
            medicalRecord.setSummary(request.getSummary());
        }
        if (request.getTreatmentNotes() != null) {
            medicalRecord.setTreatmentNotes(request.getTreatmentNotes());
        }
        if (request.getLabName() != null) {
            medicalRecord.setLabName(request.getLabName());
        }
        if (request.getResultValue() != null) {
            medicalRecord.setResultValue(request.getResultValue());
        }
        if (request.getResultUnit() != null) {
            medicalRecord.setResultUnit(request.getResultUnit());
        }
        if (request.getReferenceRange() != null) {
            medicalRecord.setReferenceRange(request.getReferenceRange());
        }
        if (request.getResultStatus() != null) {
            medicalRecord.setResultStatus(request.getResultStatus());
        }
        if (request.getAttachments() != null) {
            medicalRecord.setAttachments(request.getAttachments());
        }
        if (request.getDiagnoses() != null) {
            medicalRecord.setDiagnoses(request.getDiagnoses());
        }
        medicalRecord.setUpdatedAt(LocalDateTime.now());
        if (medicalRecord.getSummary() == null || medicalRecord.getSummary().isBlank()) {
            medicalRecord.setSummary(medicalRecord.getTreatmentNotes());
        }
        if (medicalRecord.getRecordTitle() == null || medicalRecord.getRecordTitle().isBlank()) {
            medicalRecord.setRecordTitle(resolveDefaultTitle(medicalRecord.getRecordType()));
        }

        MedicalRecord saved = medicalRecordRepository.save(medicalRecord);
        try {
            notificationService.notifyUserEvent(
                saved.getPatient().getId(),
                saved.getPatient().getEmail(),
                saved.getPatient().getFullName(),
                "medical_record.updated",
                "Medical record updated",
                String.format("Dr. %s updated medical record: %s", doctor.getFullName(), saved.getRecordTitle()),
                saved.getId()
            );
        } catch (Exception ex) {
        }
        return toResponse(saved);
    }

    public boolean softDeleteMedicalRecord(String doctorId, String recordId) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        MedicalRecord medicalRecord = medicalRecordRepository.findByIdAndIsDeletedFalse(recordId)
            .orElse(null);

        if (medicalRecord == null || medicalRecord.getDoctor() == null || !medicalRecord.getDoctor().getId().equals(doctor.getId())) {
            return false;
        }

        medicalRecord.setIsDeleted(true);
        medicalRecord.setDeletedAt(LocalDateTime.now());
        medicalRecord.setUpdatedAt(LocalDateTime.now());
        medicalRecordRepository.save(medicalRecord);
        try {
            notificationService.notifyUserEvent(
                medicalRecord.getPatient().getId(),
                medicalRecord.getPatient().getEmail(),
                medicalRecord.getPatient().getFullName(),
                "medical_record.deleted",
                "Medical record removed",
                String.format("Dr. %s removed medical record: %s", doctor.getFullName(), medicalRecord.getRecordTitle()),
                medicalRecord.getId()
            );
        } catch (Exception ex) {
        }
        return true;
    }

    public boolean hardDeleteMedicalRecord(String doctorId, String recordId) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId).orElse(null);

        if (medicalRecord == null || medicalRecord.getDoctor() == null || !medicalRecord.getDoctor().getId().equals(doctor.getId())) {
            return false;
        }

        medicalRecordRepository.delete(medicalRecord);
        try {
            notificationService.notifyUserEvent(
                medicalRecord.getPatient().getId(),
                medicalRecord.getPatient().getEmail(),
                medicalRecord.getPatient().getFullName(),
                "medical_record.hard_deleted",
                "Medical record permanently removed",
                String.format("Dr. %s permanently deleted medical record: %s", doctor.getFullName(), medicalRecord.getRecordTitle()),
                medicalRecord.getId()
            );
        } catch (Exception ex) {
        }
        return true;
    }

    public List<MedicalRecordResponse> getMyMedicalRecords(String userId) {
        User currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (currentUser.getRole() == RoleType.PATIENT) {
            return medicalRecordRepository.findAllByPatient_IdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        }

        if (currentUser.getRole() == RoleType.DOCTOR) {
            return medicalRecordRepository.findAllByDoctor_IdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        }

        throw new IllegalArgumentException("Unsupported role");
    }

    private MedicalRecordResponse toResponse(MedicalRecord medicalRecord) {
        List<String> attachmentUrls = medicalRecord.getAttachments() == null
            ? List.of()
            : medicalRecord.getAttachments().stream()
                .map(minioStorageService::toPresignedGetUrl)
                .toList();
        return new MedicalRecordResponse(medicalRecord, attachmentUrls);
    }

    private String normalizeRecordType(String recordType) {
        if (recordType == null || recordType.isBlank()) {
            return "note";
        }
        return recordType.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveRecordTitle(MedicalRecordRequest request) {
        if (request.getRecordTitle() != null && !request.getRecordTitle().isBlank()) {
            return request.getRecordTitle().trim();
        }
        return resolveDefaultTitle(normalizeRecordType(request.getRecordType()));
    }

    private String resolveDefaultTitle(String recordType) {
        return switch (normalizeRecordType(recordType)) {
            case "lab_result" -> "Lab Result";
            case "prescription" -> "Prescription";
            case "imaging" -> "Imaging Report";
            case "procedure" -> "Procedure Note";
            default -> "Clinical Note";
        };
    }

    private String resolveSummary(MedicalRecordRequest request) {
        if (request.getSummary() != null && !request.getSummary().isBlank()) {
            return request.getSummary().trim();
        }
        return request.getTreatmentNotes();
    }
}
