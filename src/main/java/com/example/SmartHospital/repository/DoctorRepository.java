package com.example.SmartHospital.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.Doctor;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, String> {
    @Query("SELECT d FROM Doctor d WHERE d.status <> :deletedStatus AND (" +
           "LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "d.phoneNumber LIKE CONCAT('%', :search, '%'))")
    Page<Doctor> searchDoctors(String search, UserStatus deletedStatus, Pageable pageable);

    Page<Doctor> findByStatusNot(UserStatus status, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusNot(UserStatus status);
}
