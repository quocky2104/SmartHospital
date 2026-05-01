package com.example.SmartHospital.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.SmartHospital.model.Prescription;
import org.springframework.stereotype.Repository;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, String> {
	Optional<Prescription> findByIdAndIsDeletedFalse(String id);
	List<Prescription> findAllByPatient_IdAndIsDeletedFalse(String patientId);
	List<Prescription> findAllByDoctor_IdAndIsDeletedFalse(String doctorId);
}
