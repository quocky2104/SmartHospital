package com.example.SmartHospital.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.SmartHospital.model.Medication;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, String> {
	Optional<Medication> findByIdAndIsDeletedFalse(String id);
	List<Medication> findAllByIsDeletedFalse();
}
