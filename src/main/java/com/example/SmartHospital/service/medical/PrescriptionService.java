package com.example.SmartHospital.service.medical;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.PrescriptionDtos.PrescriptionRequest;
import com.example.SmartHospital.dtos.PrescriptionDtos.PrescriptionResponse;
import com.example.SmartHospital.enums.RoleType;
import com.example.SmartHospital.model.Doctor;
import com.example.SmartHospital.model.Medication;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.model.Prescription;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.DoctorRepository;
import com.example.SmartHospital.repository.MedicationRepository;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.repository.PrescriptionRepository;
import com.example.SmartHospital.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrescriptionService {
    private static final String DOCTOR_NOT_FOUND = "Doctor not found";
    private static final String PATIENT_NOT_FOUND = "Patient not found";
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final MedicationRepository medicationRepository;
    private final UserRepository userRepository;

    public List<PrescriptionResponse> getMyPrescriptions(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getRole() != RoleType.PATIENT) {
            throw new IllegalArgumentException("Only patients can view their own prescriptions here");
        }

        patientRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(PATIENT_NOT_FOUND));

        return prescriptionRepository.findAllByPatient_IdAndIsDeletedFalse(userId)
            .stream()
            .map(PrescriptionResponse::new)
            .toList();
    }

    public PrescriptionResponse createPrescription(String doctorId, PrescriptionRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException(DOCTOR_NOT_FOUND));
        Patient patient = patientRepository.findById(request.getPatientId())
            .orElseThrow(() -> new IllegalArgumentException(PATIENT_NOT_FOUND));

        if (request.getMedicationIds() == null || request.getMedicationIds().isEmpty()) {
            throw new IllegalArgumentException("At least one medication is required");
        }

        List<Medication> medicines = medicationRepository.findAllById(request.getMedicationIds());
        if (medicines.size() != request.getMedicationIds().size()) {
            throw new IllegalArgumentException("One or more medication IDs are invalid");
        }

        Prescription prescription = new Prescription();
        prescription.setDoctor(doctor);
        prescription.setPatient(patient);
        prescription.setNotes(request.getNotes());
        prescription.setMedicines(medicines);
        prescription.setIsDeleted(false);

        return new PrescriptionResponse(prescriptionRepository.save(prescription));
    }

    public PrescriptionResponse updatePrescription(String doctorId, String prescriptionId, PrescriptionRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException(DOCTOR_NOT_FOUND));
        Prescription prescription = prescriptionRepository.findByIdAndIsDeletedFalse(prescriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

        if (prescription.getDoctor() == null || !prescription.getDoctor().getId().equals(doctor.getId())) {
            throw new IllegalArgumentException("Only the prescribing doctor can edit this prescription");
        }

        if (request.getPatientId() != null && !request.getPatientId().isBlank()) {
            Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(PATIENT_NOT_FOUND));
            prescription.setPatient(patient);
        }

        if (request.getMedicationIds() != null && !request.getMedicationIds().isEmpty()) {
            List<Medication> medicines = medicationRepository.findAllById(request.getMedicationIds());
            if (medicines.size() != request.getMedicationIds().size()) {
                throw new IllegalArgumentException("One or more medication IDs are invalid");
            }
            prescription.setMedicines(medicines);
        }

        if (request.getNotes() != null) {
            prescription.setNotes(request.getNotes());
        }

        prescription.setDoctor(doctor);
        prescription.setUpdatedAt(LocalDateTime.now());
        return new PrescriptionResponse(prescriptionRepository.save(prescription));
    }

    public boolean softDeletePrescription(String doctorId, String prescriptionId) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException(DOCTOR_NOT_FOUND));
        Prescription prescription = prescriptionRepository.findByIdAndIsDeletedFalse(prescriptionId)
            .orElse(null);

        if (prescription == null || prescription.getDoctor() == null || !prescription.getDoctor().getId().equals(doctor.getId())) {
            return false;
        }

        prescription.setIsDeleted(true);
        prescription.setDeletedAt(LocalDateTime.now());
        prescription.setUpdatedAt(LocalDateTime.now());
        prescriptionRepository.save(prescription);
        return true;
    }

    public boolean hardDeletePrescription(String doctorId, String prescriptionId) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException(DOCTOR_NOT_FOUND));
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
            .orElse(null);

        if (prescription == null || prescription.getDoctor() == null || !prescription.getDoctor().getId().equals(doctor.getId())) {
            return false;
        }

        prescriptionRepository.delete(prescription);
        return true;
    }

    public PrescriptionResponse getPrescriptionById(String userId, String prescriptionId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Prescription prescription = prescriptionRepository.findByIdAndIsDeletedFalse(prescriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

        boolean patientOwner = user.getRole() == RoleType.PATIENT
            && prescription.getPatient() != null
            && prescription.getPatient().getId().equals(userId);

        boolean doctorOwner = user.getRole() == RoleType.DOCTOR
            && prescription.getDoctor() != null
            && prescription.getDoctor().getId().equals(userId);

        if (!patientOwner && !doctorOwner) {
            throw new IllegalArgumentException("Access denied");
        }

        return new PrescriptionResponse(prescription);
    }

    public List<PrescriptionResponse> getMyPrescriptionsByDoctor(String doctorId) {
        return prescriptionRepository.findAllByDoctor_IdAndIsDeletedFalse(doctorId)
            .stream()
            .map(PrescriptionResponse::new)
            .toList();
    }
}