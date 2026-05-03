package com.example.SmartHospital.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.model.DoctorPatientMessages;

@Repository
public interface DoctorPatientChatRepository extends JpaRepository<DoctorPatientMessages, String> {
    List<DoctorPatientMessages> findByDoctor_IdAndPatient_IdOrderByTimestampAsc(String doctorId, String patientId);
    List<DoctorPatientMessages> findByDoctorId(String doctorId);
    List<DoctorPatientMessages> findByPatientId(String patientId);
    @Modifying
    @Query("UPDATE DoctorPatientMessages m SET m.status = com.example.SmartHospital.enums.MessageStatus.DELIVERED " +
           "WHERE m.doctor.id = :doctorId AND m.patient.id = :patientId AND m.senderId = :senderId AND m.status = com.example.SmartHospital.enums.MessageStatus.SENT")
    void markOutgoingMessagesDelivered(String doctorId, String patientId, String senderId);

    @Modifying
    @Query("UPDATE DoctorPatientMessages m SET m.status = com.example.SmartHospital.enums.MessageStatus.READ " +
           "WHERE m.doctor.id = :doctorId AND m.patient.id = :patientId AND m.senderId = :senderId " +
           "AND m.status IN (com.example.SmartHospital.enums.MessageStatus.SENT, com.example.SmartHospital.enums.MessageStatus.DELIVERED)")
    void markOutgoingMessagesRead(String doctorId, String patientId, String senderId);

    @Modifying
    @Query("UPDATE DoctorPatientMessages m SET m.status = com.example.SmartHospital.enums.MessageStatus.DELIVERED " +
           "WHERE m.doctor.id = :doctorId AND m.senderId <> :doctorId AND m.status = com.example.SmartHospital.enums.MessageStatus.SENT")
    void markDeliveredForRecipientDoctor(String doctorId);

    @Modifying
    @Query("UPDATE DoctorPatientMessages m SET m.status = com.example.SmartHospital.enums.MessageStatus.DELIVERED " +
           "WHERE m.patient.id = :patientId AND m.senderId <> :patientId AND m.status = com.example.SmartHospital.enums.MessageStatus.SENT")
    void markDeliveredForRecipientPatient(String patientId);

    @Query("SELECT DISTINCT m.senderId FROM DoctorPatientMessages m WHERE m.doctor.id = :doctorId AND m.senderId <> :doctorId AND m.status = com.example.SmartHospital.enums.MessageStatus.SENT")
    List<String> findSendersWithSentMessagesToDoctor(String doctorId);

    @Query("SELECT DISTINCT m.senderId FROM DoctorPatientMessages m WHERE m.patient.id = :patientId AND m.senderId <> :patientId AND m.status = com.example.SmartHospital.enums.MessageStatus.SENT")
    List<String> findSendersWithSentMessagesToPatient(String patientId);
}
