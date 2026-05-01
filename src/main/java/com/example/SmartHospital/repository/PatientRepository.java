package com.example.SmartHospital.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {
    boolean existsByInsuranceNumber(String insuranceNumber);
    
    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "p.phoneNumber LIKE CONCAT('%', :search, '%') OR " +
           "p.identityNumber LIKE CONCAT('%', :search, '%')")
    Page<Patient> searchPatients(@Param("search") String search, Pageable pageable);
    
    @Override
    Page<Patient> findAll(Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusNot(UserStatus status);
}
