package com.example.SmartHospital.repository;

import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.SmartHospital.model.DoctorChatbotMessages;

@Repository
public interface DoctorChatbotChatRepository extends JpaRepository<DoctorChatbotMessages, String> {
    List<DoctorChatbotMessages> findByDoctorIdOrderByTimestampAsc(String doctorId);
}
