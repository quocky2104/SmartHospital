package com.example.SmartHospital.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import com.example.SmartHospital.enums.GenderType;
import com.example.SmartHospital.enums.RoleType;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

@Entity
@Data
@Inheritance(strategy = InheritanceType.JOINED) //this is a parent class -> store different tables but join them by id
@Table(name = "users")
public class User {
    @Id
    private String id;

    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generateUserId();
        }
        this.createdAt = LocalDateTime.now();
    }

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = true)
    private String firstName;

    @Column(nullable = true)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private GenderType gender;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private RoleType role;

    @Column(nullable = false, unique = true)
    private String identityNumber;

    private String address;

    private String city;

    private String zipCode;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(nullable = false)
    private String hashedPassword;

    private String avatarPath;

    // Derived attribute
    @Transient
    public Integer getAge() {
        if (this.dateOfBirth == null) {
            return null;
        }
        return LocalDate.now().getYear() - this.dateOfBirth.getYear();
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

    @Column(nullable = false)
    @ColumnDefault("false") 
    private Boolean isDeleted = false;
}
