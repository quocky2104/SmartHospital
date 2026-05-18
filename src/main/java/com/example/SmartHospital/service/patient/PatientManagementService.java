package com.example.SmartHospital.service.patient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.RegisterRequest;
import com.example.SmartHospital.dtos.PaginatedResponse;
import com.example.SmartHospital.dtos.UserDtos.EditProfile.PatientEditProfileRequest;
import com.example.SmartHospital.dtos.UserDtos.PatientDTO;
import com.example.SmartHospital.enums.RoleType;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.EmergencyContact;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.repository.UserRepository;
import com.example.SmartHospital.service.storage.MinioStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientManagementService {
    private final PatientRepository patientRepository;
    private final MinioStorageService minioStorageService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
            .toList();

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

    public PatientDTO createPatient(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists");
        }
        if (userRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            throw new IllegalArgumentException("Identity number already exists");
        }

        Patient patient = new Patient();
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setFullName(request.getFirstName() + " " + request.getLastName());
        patient.setEmail(request.getEmail());
        patient.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setIdentityNumber(request.getIdentityNumber());
        patient.setAddress(request.getAddress());
        patient.setCity(request.getCity());
        patient.setZipCode(null);
        patient.setGender(request.getGender());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setRole(RoleType.PATIENT);
        patient.setStatus(UserStatus.ACTIVE);
        patient.setInsuranceNumber(request.getInsuranceNumber());
        patient.setInsuranceProvider(request.getInsuranceProvider());
        patient.setAvatarPath(request.getAvatarPath());
        if (request.getEmergencyContacts() != null && !request.getEmergencyContacts().isEmpty()) {
            patient.setEmergencyContacts(
                request.getEmergencyContacts().stream()
                    .map(ec -> new EmergencyContact(ec.getPhoneNumber(), ec.getRelationship()))
                    .collect(Collectors.toList())
            );
        }
        patientRepository.save(patient);
        return convertToPatientDTO(patient);
    }

    public PatientDTO editPatientByAdmin(String patientId, PatientEditProfileRequest request) {
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null || patient.getStatus() == UserStatus.DELETED) {
            return null;
        }

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(patient.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }
            patient.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(patient.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new IllegalArgumentException("Phone number already exists");
            }
            patient.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getIdentityNumber() != null && !request.getIdentityNumber().equals(patient.getIdentityNumber())) {
            if (userRepository.existsByIdentityNumber(request.getIdentityNumber())) {
                throw new IllegalArgumentException("Identity number already exists");
            }
            patient.setIdentityNumber(request.getIdentityNumber());
        }

        if (request.getFirstName() != null) patient.setFirstName(request.getFirstName());
        if (request.getLastName() != null) patient.setLastName(request.getLastName());
        if (request.getFirstName() != null || request.getLastName() != null) {
            String fullName = ((patient.getFirstName() == null ? "" : patient.getFirstName()) + " " + (patient.getLastName() == null ? "" : patient.getLastName())).trim();
            patient.setFullName(fullName);
        }
        if (request.getGender() != null) patient.setGender(request.getGender());
        if (request.getDateOfBirth() != null) patient.setDateOfBirth(request.getDateOfBirth());
        if (request.getAddress() != null) patient.setAddress(request.getAddress());
        if (request.getCity() != null) patient.setCity(request.getCity());
        if (request.getZipCode() != null) patient.setZipCode(request.getZipCode());
        if (request.getStatus() != null) patient.setStatus(request.getStatus());
        if (request.getInsuranceNumber() != null) patient.setInsuranceNumber(request.getInsuranceNumber());
        if (request.getInsuranceId() != null) patient.setInsuranceId(request.getInsuranceId());
        if (request.getInsuranceProvider() != null) patient.setInsuranceProvider(request.getInsuranceProvider());
        if (request.getBloodType() != null) patient.setBloodType(request.getBloodType());

        if (request.getEmergencyContacts() != null) {
            patient.setEmergencyContacts(
                request.getEmergencyContacts().stream()
                    .map(ec -> new EmergencyContact(ec.getPhoneNumber(), ec.getRelationship()))
                    .collect(Collectors.toList())
            );
        }

        patientRepository.save(patient);
        return convertToPatientDTO(patient);
    }

    public PatientDTO editPatientProfile(
        PatientEditProfileRequest request,
        String userId,
        MultipartFile avatarFile,
        List<MultipartFile> additionalFiles,
        List<String> removeAdditionalFiles
    ) {
        Patient patient = patientRepository.findById(userId).orElse(null);
        if (patient == null || patient.getStatus() == UserStatus.DELETED) {
            return null;
        }
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setFullName(request.getFirstName() + " " + request.getLastName());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setAddress(request.getAddress());
        patient.setCity(request.getCity());
        patient.setZipCode(request.getZipCode());
        patient.setGender(request.getGender());
        String avatarPath = minioStorageService.uploadAvatar(avatarFile, patient.getId());
        if (avatarPath != null) {
            patient.setAvatarPath(avatarPath);
        }
        patient.setInsuranceNumber(request.getInsuranceNumber());
        patient.setInsuranceId(request.getInsuranceId());
        patient.setInsuranceProvider(request.getInsuranceProvider());
        patient.setBloodType(request.getBloodType());

        // Map emergency contacts if provided
        if (request.getEmergencyContacts() != null && !request.getEmergencyContacts().isEmpty()) {
            patient.setEmergencyContacts(
                request.getEmergencyContacts().stream()
                    .map(ec -> new EmergencyContact(ec.getPhoneNumber(), ec.getRelationship()))
                    .collect(Collectors.toList())
            );
        }

        List<String> currentAdditionalFiles = patient.getAdditionalFiles() == null
            ? new ArrayList<>()
            : new ArrayList<>(patient.getAdditionalFiles());

        if (removeAdditionalFiles != null && !removeAdditionalFiles.isEmpty()) {
            minioStorageService.deleteFiles(removeAdditionalFiles);
            currentAdditionalFiles.removeAll(removeAdditionalFiles);
        }

        List<String> uploadedAdditionalFiles = minioStorageService.uploadAdditionalFiles(additionalFiles, patient.getId());
        if (!uploadedAdditionalFiles.isEmpty()) {
            currentAdditionalFiles.addAll(uploadedAdditionalFiles);
        }

        patient.setAdditionalFiles(currentAdditionalFiles);
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
        dto.setId(patient.getId());
        dto.setEmail(patient.getEmail());
        dto.setFullName(patient.getFullName());
        dto.setFirstName(patient.getFirstName());
        dto.setLastName(patient.getLastName());
        dto.setPhoneNumber(patient.getPhoneNumber());
        dto.setIdentityNumber(patient.getIdentityNumber());
        dto.setGender(patient.getGender());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setAddress(patient.getAddress());
        dto.setCity(patient.getCity());
        dto.setZipCode(patient.getZipCode());
        // Return a presigned MinIO URL for API consumers while DB stores the bucket/object path
        String storedAvatar = patient.getAvatarPath();
        dto.setAvatarPath(minioStorageService.toPresignedGetUrl(storedAvatar));
        dto.setStatus(patient.getStatus());
        dto.setInsuranceNumber(patient.getInsuranceNumber());
        dto.setInsuranceId(patient.getInsuranceId());
        dto.setInsuranceProvider(patient.getInsuranceProvider());
        dto.setBloodType(patient.getBloodType());
        dto.setEmergencyContacts(patient.getEmergencyContacts());
        dto.setAdditionalFiles(patient.getAdditionalFiles());
        dto.setTwoFactorEnabled(patient.getTwoFactorEnabled());
        dto.setCreatedAt(patient.getCreatedAt() == null ? null : patient.getCreatedAt().toString());
        return dto;
    }
}