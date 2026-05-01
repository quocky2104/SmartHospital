package com.example.SmartHospital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.SmartHospital.model.MedicalRecord;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, String> {
	Optional<MedicalRecord> findByIdAndIsDeletedFalse(String id);
	List<MedicalRecord> findAllByPatient_IdAndIsDeletedFalse(String patientId);
	List<MedicalRecord> findAllByDoctor_IdAndIsDeletedFalse(String doctorId);
}
