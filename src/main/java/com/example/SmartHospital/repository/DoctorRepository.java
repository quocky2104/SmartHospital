package com.example.SmartHospital.repository;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.Doctor;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, String> {
    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "d.phoneNumber LIKE CONCAT('%', :search, '%') OR " +
           "d.specialization LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Doctor> searchDoctors(String search, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusNot(UserStatus status);
}
