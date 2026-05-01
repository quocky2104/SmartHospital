package com.example.SmartHospital.service.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.RegisterRequest;
import com.example.SmartHospital.enums.RoleType;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.helper.CustomIdGenerator;
import com.example.SmartHospital.model.MedicalRecord;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.repository.UserRepository;
import com.example.SmartHospital.service.storage.MinioStorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioStorageService minioStorageService;
    
    @Transactional
    public void updateLastLogin(String email) {
        userRepository.updateLastLogin(email);
    }

    public User registerUser(
        RegisterRequest registerRequest,
        MultipartFile avatarFile,
        List<MultipartFile> medicalRecordFiles
    ) {

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        } 
        if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists");
        }
        if(userRepository.existsByIdentityNumber(registerRequest.getIdentityNumber())) {
            throw new IllegalArgumentException("Identity number already exists");
        }
        if(patientRepository.existsByInsuranceNumber(registerRequest.getInsuranceNumber())) {
            throw new IllegalArgumentException("Insurance number already exists");
        }

        //check phone number is it valid or not
        if(!registerRequest.getPhoneNumber().matches("\\d{10,15}")) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        //check identity number is it valid or not
        if(!registerRequest.getIdentityNumber().matches("\\w{5,20}")) {
            throw new IllegalArgumentException("Invalid identity number");
        }
        // check insurance number is it valid or not
        if(registerRequest.getInsuranceNumber() != null && 
           !registerRequest.getInsuranceNumber().matches("\\w{5,20}")) {
            throw new IllegalArgumentException("Invalid insurance number");
        }
        //check if email is valid or not
        if(!registerRequest.getEmail().matches("^[A-Za-z0-9+_.%-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        //check if password is strong enough, 1 uppercase, 1 lowercase, 1 digit, 1 special character, at least 8 characters
        if(registerRequest.getPassword().length() < 8 ||
           !registerRequest.getPassword().matches(".*[A-Z].*") ||
           !registerRequest.getPassword().matches(".*[a-z].*") ||
           !registerRequest.getPassword().matches(".*\\d.*") ||
           !registerRequest.getPassword().matches(".*[!@#$%^&*()].*")) {
            throw new IllegalArgumentException("Password is not strong enough! 1 uppercase, 1 lowercase, 1 digit, 1 special character, at least 8 characters");
        }

        Patient patient = new Patient();
        patient.setId(CustomIdGenerator.generateUserId());
        patient.setEmail(registerRequest.getEmail());
        patient.setHashedPassword(passwordEncoder.encode(registerRequest.getPassword()));
        patient.setFullName(registerRequest.getName());
        patient.setPhoneNumber(registerRequest.getPhoneNumber());
        patient.setIdentityNumber(registerRequest.getIdentityNumber());
        patient.setGender(registerRequest.getGender());
        patient.setDateOfBirth(registerRequest.getDateOfBirth());
        patient.setAddress(registerRequest.getAddress());
        patient.setStatus(UserStatus.ACTIVE);
        patient.setRole(RoleType.PATIENT);
        patient.setInsuranceNumber(registerRequest.getInsuranceNumber());
        patient.setInsuranceProvider(registerRequest.getInsuranceProvider());

        String avatarPath = minioStorageService.uploadAvatar(avatarFile, patient.getId());
        patient.setAvatarPath(avatarPath != null ? avatarPath : registerRequest.getAvatarPath());

        List<MedicalRecord> medicalRecords = new ArrayList<>();
        List<String> uploadedMedicalRecordPaths = minioStorageService.uploadMedicalRecordPdfs(medicalRecordFiles, patient.getId());
        if (!uploadedMedicalRecordPaths.isEmpty()) {
            MedicalRecord medicalRecord = new MedicalRecord();
            medicalRecord.setPatient(patient);
            medicalRecord.setAttachments(uploadedMedicalRecordPaths);
            medicalRecords.add(medicalRecord);
        }
        
        patient.setMedicalRecords(medicalRecords);

        return patientRepository.save(patient);
    }


}

