package com.example.SmartHospital.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.SmartHospital.model.Reminder;

public interface ReminderRepository extends JpaRepository<Reminder, String> {
    List<Reminder> findByMedicationIdAndIsDeletedFalse(String medicationId);
}
