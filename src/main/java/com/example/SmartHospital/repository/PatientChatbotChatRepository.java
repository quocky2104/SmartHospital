package com.example.SmartHospital.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.SmartHospital.model.PatientChatbotMessages;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientChatbotChatRepository extends JpaRepository<PatientChatbotMessages, String> {
    List<PatientChatbotMessages> findByPatientIdOrderByTimestampAsc(String patientId);
}
