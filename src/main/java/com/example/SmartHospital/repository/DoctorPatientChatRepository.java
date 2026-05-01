package com.example.SmartHospital.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.enums.MessageStatus;
import com.example.SmartHospital.model.DoctorPatientMessages;

@Repository
public interface DoctorPatientChatRepository extends JpaRepository<DoctorPatientMessages, String> {
    List<DoctorPatientMessages> findByDoctor_IdAndPatient_IdOrderByTimestampAsc(String doctorId, String patientId);
    List<DoctorPatientMessages> findByDoctorId(String doctorId);
    List<DoctorPatientMessages> findByPatientId(String patientId);
    @Modifying
    @Query("UPDATE DoctorPatientMessages m SET m.status = :status WHERE m.doctor.id = :doctorId AND m.patient.id = :patientId")
    void updateMessagesStatusByDoctorAndPatient(String doctorId, String patientId, MessageStatus status);
}
