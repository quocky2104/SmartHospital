package com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests;


import java.time.LocalDate;
import java.util.List;

import com.example.SmartHospital.enums.GenderType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
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

    private String zipCode; //optional

    @NotNull
    private GenderType gender;

    @NotNull
    private LocalDate dateOfBirth;

    private String insuranceNumber; //optional
    private String insuranceId; //optional
    private String insuranceProvider; //optional
    private String bloodType; //optional
    private String avatarPath; //optional
    
    private List<EmergencyContactRequest> emergencyContacts; //optional
}
