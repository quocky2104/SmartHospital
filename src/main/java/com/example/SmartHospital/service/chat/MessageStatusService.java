package com.example.SmartHospital.service.chat;
import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.enums.MessageStatus;
import com.example.SmartHospital.repository.DoctorPatientChatRepository;
import com.example.SmartHospital.service.messaging.WebSocketMessagingService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageStatusService {
    private static final String ROLE_DOCTOR = "ROLE_DOCTOR";

    private final DoctorPatientChatRepository chatRepository;
    private final WebSocketMessagingService websocketMessagingService;

    // Traansactional is important to ensure that the status update 
    // and the notification happen atomically, preventing race conditions 
    // where the notification is sent before the status is updated in the 
    // database. This ensures data consistency and that the receiver 
    // gets accurate status updates in real-time
    @Transactional
    public void markAsRead(Principal principal, String otherUserId) {
        String userId = principal.getName();
        boolean isDoctor = principal instanceof Authentication authentication
                && authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ROLE_DOCTOR));

        String doctorId = isDoctor ? userId : otherUserId;
        String patientId = isDoctor ? otherUserId : userId;

        chatRepository.updateMessagesStatusByDoctorAndPatient(doctorId, patientId, MessageStatus.READ);
        websocketMessagingService.sendMessageStatus(userId, otherUserId, MessageStatus.READ);
    }

    @Transactional
    public void markAsDelivered(Principal principal, String otherUserId) {
        String userId = principal.getName();
        boolean isDoctor = principal instanceof Authentication authentication
                && authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ROLE_DOCTOR));

        String doctorId = isDoctor ? userId : otherUserId;
        String patientId = isDoctor ? otherUserId : userId;

        chatRepository.updateMessagesStatusByDoctorAndPatient(doctorId, patientId, MessageStatus.DELIVERED);
        websocketMessagingService.sendMessageStatus(userId, otherUserId, MessageStatus.DELIVERED);
    }

    @Transactional
    public void markAsSent(Principal principal, String otherUserId) {
        String userId = principal.getName();
        boolean isDoctor = principal instanceof Authentication authentication
                && authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ROLE_DOCTOR));

        String doctorId = isDoctor ? userId : otherUserId;
        String patientId = isDoctor ? otherUserId : userId;

        chatRepository.updateMessagesStatusByDoctorAndPatient(doctorId, patientId, MessageStatus.SENT);
        websocketMessagingService.sendMessageStatus(userId, otherUserId, MessageStatus.SENT);
    }
}
