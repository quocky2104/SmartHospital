package com.example.SmartHospital.service.medical;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.MedicationDtos.MedicationRequest;
import com.example.SmartHospital.dtos.MedicationDtos.MedicationResponse;
import com.example.SmartHospital.model.Medication;
import com.example.SmartHospital.model.Prescription;
import com.example.SmartHospital.repository.MedicationRepository;
import com.example.SmartHospital.repository.PrescriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MedicationService {
    private final MedicationRepository medicationRepository;
    private final PrescriptionRepository prescriptionRepository;

    public List<MedicationResponse> getAllMedications() {
        return medicationRepository.findAllByIsDeletedFalse().stream().map(MedicationResponse::new).toList();
    }

    public MedicationResponse getMedicationById(String medicationId) {
        Medication medication = medicationRepository.findByIdAndIsDeletedFalse(medicationId)
            .orElseThrow(() -> new IllegalArgumentException("Medication not found"));
        return new MedicationResponse(medication);
    }

    public MedicationResponse createMedication(MedicationRequest request) {
        Medication medication = new Medication();
        medication.setName(request.getName());
        medication.setDosage(request.getDosage());
        medication.setDuration(request.getDuration());
        medication.setFrequency(request.getFrequency());
        medication.setIsDeleted(false);
        return new MedicationResponse(medicationRepository.save(medication));
    }

    public MedicationResponse updateMedication(String medicationId, MedicationRequest request) {
        Medication medication = medicationRepository.findByIdAndIsDeletedFalse(medicationId)
            .orElseThrow(() -> new IllegalArgumentException("Medication not found"));
        medication.setName(request.getName());
        medication.setDosage(request.getDosage());
        medication.setDuration(request.getDuration());
        medication.setFrequency(request.getFrequency());
        medication.setUpdatedAt(LocalDateTime.now());
        return new MedicationResponse(medicationRepository.save(medication));
    }

    public boolean softDeleteMedication(String medicationId) {
        Medication medication = medicationRepository.findByIdAndIsDeletedFalse(medicationId)
            .orElse(null);
        if (medication == null) {
            return false;
        }
        medication.setIsDeleted(true);
        medication.setDeletedAt(LocalDateTime.now());
        medication.setUpdatedAt(LocalDateTime.now());
        medicationRepository.save(medication);
        return true;
    }

    public boolean hardDeleteMedication(String medicationId) {
        Medication medication = medicationRepository.findById(medicationId)
            .orElse(null);
        if (medication == null) {
            return false;
        }

        for (Prescription prescription : prescriptionRepository.findAll()) {
            if (prescription.getMedicines() != null && prescription.getMedicines().removeIf(m -> m.getId().equals(medicationId))) {
                prescriptionRepository.save(prescription);
            }
        }

        medicationRepository.delete(medication);
        return true;
    }
}