package com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests;


import java.time.LocalDate;

import com.example.SmartHospital.enums.GenderType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;
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
    @NotNull
    private GenderType gender;
    @NotNull
    private LocalDate dateOfBirth;

    private String insuranceNumber; //optional
    private String insuranceProvider; //optional
    private String avatarPath; //optional
    private String medicalRecordPath;
}
