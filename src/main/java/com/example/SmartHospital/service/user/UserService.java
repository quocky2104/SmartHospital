package com.example.SmartHospital.service.user;

import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.RegisterRequest;
import com.example.SmartHospital.enums.RoleType;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.helper.CustomIdGenerator;
import com.example.SmartHospital.model.EmergencyContact;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public void updateLastLogin(String email) {
        userRepository.updateLastLogin(email);
    }

    public User registerUser(RegisterRequest registerRequest) {

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        } 
        if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists");
        }
        if(userRepository.existsByIdentityNumber(registerRequest.getIdentityNumber())) {
            throw new IllegalArgumentException("Identity number already exists");
        }

        //check phone number is it valid or not
        if(!registerRequest.getPhoneNumber().matches("\\d{10,15}")) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        //check identity number is it valid or not
        if(!registerRequest.getIdentityNumber().matches("\\w{5,20}")) {
            throw new IllegalArgumentException("Invalid identity number");
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
        patient.setFirstName(registerRequest.getFirstName());
        patient.setLastName(registerRequest.getLastName());
        patient.setFullName(registerRequest.getFirstName() + " " + registerRequest.getLastName());
        patient.setPhoneNumber(registerRequest.getPhoneNumber());
        patient.setIdentityNumber(registerRequest.getIdentityNumber());
        patient.setGender(registerRequest.getGender());
        patient.setDateOfBirth(registerRequest.getDateOfBirth());
        patient.setAddress(registerRequest.getAddress());
        patient.setCity(registerRequest.getCity());
        patient.setZipCode(registerRequest.getZipCode());
        patient.setStatus(UserStatus.ACTIVE);
        patient.setRole(RoleType.PATIENT);
        patient.setInsuranceNumber(registerRequest.getInsuranceNumber());
        patient.setInsuranceId(registerRequest.getInsuranceId());
        patient.setInsuranceProvider(registerRequest.getInsuranceProvider());
        patient.setBloodType(registerRequest.getBloodType());
        patient.setAvatarPath(registerRequest.getAvatarPath());
        
        // Map emergency contacts if provided
        if (registerRequest.getEmergencyContacts() != null && !registerRequest.getEmergencyContacts().isEmpty()) {
            patient.setEmergencyContacts(
                registerRequest.getEmergencyContacts().stream()
                    .map(ec -> new EmergencyContact(ec.getPhoneNumber(), ec.getRelationship()))
                    .collect(Collectors.toList())
            );
        }

        return patientRepository.save(patient);
    }


}

