package com.example.SmartHospital.dtos.UserDtos;

import java.time.LocalDate;
import java.util.List;

import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.EmergencyContactRequest;
import com.example.SmartHospital.enums.GenderType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatientCreateRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String identityNumber;

    @NotBlank
    private String address;

    @NotBlank
    private String city;

    private String zipCode;

    @NotNull
    private GenderType gender;

    @NotNull
    private LocalDate dateOfBirth;

    private String insuranceNumber;
    private String insuranceId;
    private String insuranceProvider;
    private String bloodType;
    private List<EmergencyContactRequest> emergencyContacts;
}