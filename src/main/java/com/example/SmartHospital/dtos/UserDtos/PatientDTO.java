package com.example.SmartHospital.dtos.UserDtos;

import java.time.LocalDate;
import java.util.List;

import com.example.SmartHospital.enums.GenderType;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.EmergencyContact;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Default constructor
@AllArgsConstructor // All arguments constructor
public class PatientDTO {
    private String id;
    private String email;
    private String fullName;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String identityNumber;
    private GenderType gender;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String zipCode;
    private String avatarPath;
    private List<String> additionalFiles;
    private UserStatus status;
    private String insuranceNumber;
    private String insuranceId;
    private String insuranceProvider;
    private String bloodType;
    private List<EmergencyContact> emergencyContacts;
    private Boolean twoFactorEnabled;
    private String createdAt;
}
