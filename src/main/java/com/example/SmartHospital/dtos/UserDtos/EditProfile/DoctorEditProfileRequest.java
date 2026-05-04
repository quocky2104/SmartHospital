package com.example.SmartHospital.dtos.UserDtos.EditProfile;

import java.time.LocalDate;

import com.example.SmartHospital.enums.GenderType;
import com.example.SmartHospital.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorEditProfileRequest {
    private String email;
    private String fullName;
    private String address;
    private GenderType gender;
    private LocalDate dateOfBirth;
    private UserStatus status;
    private String phoneNumber;
    private String identityNumber;
    private String workingHours;
    private String availabilityStatus;
    private String departmentId;
}
