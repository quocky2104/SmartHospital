package com.example.SmartHospital.service.medical;

import java.time.LocalDateTime;
import java.util.List;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

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

        return new MedicalRecordResponse(medicalRecord);
    }

    public MedicalRecordResponse createMedicalRecord(String doctorId, MedicalRecordRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setDoctor(doctor);
        medicalRecord.setPatient(patient);
        medicalRecord.setTreatmentNotes(request.getTreatmentNotes());
        medicalRecord.setAttachments(request.getAttachments());
        medicalRecord.setDiagnoses(request.getDiagnoses());
        medicalRecord.setIsDeleted(false);

        return new MedicalRecordResponse(medicalRecordRepository.save(medicalRecord));
    }

    public MedicalRecordResponse updateMedicalRecord(String doctorId, String recordId, MedicalRecordRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        MedicalRecord medicalRecord = medicalRecordRepository.findByIdAndIsDeletedFalse(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Medical record not found"));

        if (medicalRecord.getDoctor() == null || !medicalRecord.getDoctor().getId().equals(doctor.getId())) {
            throw new IllegalArgumentException("Only the doctor in charge can edit this medical record");
        }

        if (request.getTreatmentNotes() != null) {
            medicalRecord.setTreatmentNotes(request.getTreatmentNotes());
        }
        if (request.getAttachments() != null) {
            medicalRecord.setAttachments(request.getAttachments());
        }
        if (request.getDiagnoses() != null) {
            medicalRecord.setDiagnoses(request.getDiagnoses());
        }
        medicalRecord.setUpdatedAt(LocalDateTime.now());

        return new MedicalRecordResponse(medicalRecordRepository.save(medicalRecord));
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
        return true;
    }

    public List<MedicalRecordResponse> getMyMedicalRecords(String userId) {
        User currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (currentUser.getRole() == RoleType.PATIENT) {
            return medicalRecordRepository.findAllByPatient_IdAndIsDeletedFalse(userId)
                .stream()
                .map(MedicalRecordResponse::new)
                .toList();
        }

        if (currentUser.getRole() == RoleType.DOCTOR) {
            return medicalRecordRepository.findAllByDoctor_IdAndIsDeletedFalse(userId)
                .stream()
                .map(MedicalRecordResponse::new)
                .toList();
        }

        throw new IllegalArgumentException("Unsupported role");
    }
}
