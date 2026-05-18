package com.example.SmartHospital.dtos.UserDtos;

import java.time.LocalDate;

import com.example.SmartHospital.enums.GenderType;
import com.example.SmartHospital.model.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileDTO {
    private String id;
    private String email;
    private String fullName;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String identityNumber;
    private LocalDate dateOfBirth;
    private GenderType gender;
    private String address;
    private String city;
    private String zipCode;
    private String avatarPath;
    private String role;
    private Boolean twoFactorEnabled;

    public static AdminProfileDTO fromUser(User user) {
        return new AdminProfileDTO(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhoneNumber(),
            user.getIdentityNumber(),
            user.getDateOfBirth(),
            user.getGender(),
            user.getAddress(),
            user.getCity(),
            user.getZipCode(),
            user.getAvatarPath(),
            user.getRole() != null ? user.getRole().name() : null,
            user.getTwoFactorEnabled()
        );
    }
}
