package com.example.SmartHospital.dtos.UserDtos;

import java.time.LocalDate;

import com.example.SmartHospital.enums.GenderType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCreateRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Identity number is required")
    private String identityNumber;

    @NotNull(message = "Gender is required")
    private GenderType gender;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Working hours is required")
    private String workingHours;

    @NotBlank(message = "Availability status is required")
    private String availabilityStatus;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotBlank(message = "Department ID is required")
    private String departmentId;
}
