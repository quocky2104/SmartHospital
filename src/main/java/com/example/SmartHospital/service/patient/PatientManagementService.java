package com.example.SmartHospital.service.patient;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.SmartHospital.dtos.PaginatedResponse;
import com.example.SmartHospital.dtos.UserDtos.EditProfile.PatientEditProfileRequest;
import com.example.SmartHospital.dtos.UserDtos.PatientDTO;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.repository.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientManagementService {
    private final PatientRepository patientRepository;

    // Paginated retrieval of patients with optional search
    public PaginatedResponse<PatientDTO> getPatients(int pageNumber, int pageSize, String search) {
        if (pageNumber < 0) {
            pageNumber = 0;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        if (pageSize > 10) {
            pageSize = 10;
        }

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Patient> patientPage;
        if (search != null && !search.trim().isEmpty()) {
            patientPage = patientRepository.searchPatients(search.trim(), pageable);
        } else {
            patientPage = patientRepository.findAll(pageable);
        }

        List<PatientDTO> content = patientPage.getContent().stream()
            .filter(patient -> patient.getStatus() != UserStatus.DELETED)
            .map(this::convertToPatientDTO)
            .collect(Collectors.toList());

        return new PaginatedResponse<>(
            content,
            patientPage.getNumber(),
            patientPage.getSize(),
            patientPage.getTotalElements(),
            patientPage.getTotalPages(),
            patientPage.isLast()
        );
    }

    public PatientDTO getPatientById(String id) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient == null || patient.getStatus() == UserStatus.DELETED) {
            return null;
        }
        return convertToPatientDTO(patient);
    }

    public PatientDTO editPatientProfile(PatientEditProfileRequest request, String userId, MultipartFile avatarFile) {
        Patient patient = patientRepository.findById(userId).orElse(null);
        if (patient == null || patient.getStatus() == UserStatus.DELETED) {
            return null;
        }
        patient.setFullName(request.getFullName());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setAddress(request.getAddress());
        patient.setAvatarPath(saveAvatarFile(avatarFile));
        patient.setInsuranceNumber(request.getInsuranceNumber());
        patient.setInsuranceProvider(request.getInsuranceProvider());
        patientRepository.save(patient);
        return convertToPatientDTO(patient);
    }

    public boolean softDeletePatient(String patientId) {
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null || patient.getStatus() == UserStatus.DELETED) {
            return false;
        }
        patient.setStatus(UserStatus.DELETED);
        patient.setIsDeleted(true);
        patientRepository.save(patient);
        return true;
    }

    public boolean hardDeletePatient(String patientId) {
        if (!patientRepository.existsById(patientId)) {
            return false;
        }
        patientRepository.deleteById(patientId);
        return true;
    }

    private PatientDTO convertToPatientDTO(Patient patient) {
        PatientDTO dto = new PatientDTO();
        dto.setEmail(patient.getEmail());
        dto.setFullName(patient.getFullName());
        dto.setPhoneNumber(patient.getPhoneNumber());
        dto.setIdentityNumber(patient.getIdentityNumber());
        dto.setGender(patient.getGender());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setAddress(patient.getAddress());
        dto.setAvatarPath(patient.getAvatarPath());
        dto.setStatus(patient.getStatus());
        dto.setInsuranceNumber(patient.getInsuranceNumber());
        dto.setInsuranceProvider(patient.getInsuranceProvider());
        return dto;
    }

    private String saveAvatarFile(MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return null;
        }
        return "path/to/saved/avatar.jpg";
    }
}