package com.example.SmartHospital.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.model.PatientMedicalRequest;

@Repository
public interface PatientMedicalRequestRepository extends JpaRepository<PatientMedicalRequest, String> {

    List<PatientMedicalRequest> findByPatient_IdOrderByCreatedAtDesc(String patientId);

    List<PatientMedicalRequest> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT r FROM PatientMedicalRequest r
        JOIN Appointment a ON a.patient.id = r.patient.id
        WHERE a.doctor.id = :doctorId
        ORDER BY r.createdAt DESC
    """)
    List<PatientMedicalRequest> findByDoctorAppointments(@org.springframework.data.repository.query.Param("doctorId") String doctorId);
}
