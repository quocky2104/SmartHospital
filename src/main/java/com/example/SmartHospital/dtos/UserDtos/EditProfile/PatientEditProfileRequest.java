package com.example.SmartHospital.dtos.UserDtos.EditProfile;
import java.time.LocalDate;
import java.util.List;

import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.EmergencyContactRequest;
import com.example.SmartHospital.enums.GenderType;
import com.example.SmartHospital.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientEditProfileRequest {
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
    private UserStatus status;
    private String insuranceNumber;
    private String insuranceId;
    private String insuranceProvider;
    private String bloodType;
    private List<EmergencyContactRequest> emergencyContacts;
}
