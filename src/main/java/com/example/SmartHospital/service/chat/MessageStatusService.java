package com.example.SmartHospital.service.chat;
import java.security.Principal;
import java.util.Map;

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
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageStatusService.class);

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

        chatRepository.markOutgoingMessagesRead(doctorId, patientId, userId);
        websocketMessagingService.sendMessageStatus(userId, otherUserId, Map.of("status", MessageStatus.READ.name(), "otherUserId", userId));
    }

    @Transactional
    public void markAsDelivered(Principal principal, String otherUserId) {
        String userId = principal.getName();
        boolean isDoctor = principal instanceof Authentication authentication
                && authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ROLE_DOCTOR));

        String doctorId = isDoctor ? userId : otherUserId;
        String patientId = isDoctor ? otherUserId : userId;

        chatRepository.markOutgoingMessagesDelivered(doctorId, patientId, userId);
        websocketMessagingService.sendMessageStatus(userId, otherUserId, Map.of("status", MessageStatus.DELIVERED.name(), "otherUserId", userId));
    }

    @Transactional
    public void markAsSent(Principal principal, String otherUserId) {
        websocketMessagingService.sendMessageStatus(principal.getName(), otherUserId, Map.of("status", MessageStatus.SENT.name(), "otherUserId", principal.getName()));
    }

    public void sendTypingIndicator(String senderId, String receiverId, boolean isTyping) {
        websocketMessagingService.sendTypingIndicator(senderId, receiverId, Map.of(
            "senderId", senderId,
            "receiverId", receiverId,
            "isTyping", isTyping
        ));
    }

    @Transactional
    public void handleUserConnected(String userId) {
        // When a user connects, mark any messages addressed to them that are still SENT as DELIVERED
        try {
            // For messages where the connected user is the doctor
            java.util.List<String> doctorSenders = chatRepository.findSendersWithSentMessagesToDoctor(userId);
            if (!doctorSenders.isEmpty()) {
                chatRepository.markDeliveredForRecipientDoctor(userId);
                for (String senderId : doctorSenders) {
                    websocketMessagingService.sendMessageStatus(userId, senderId, Map.of("status", MessageStatus.DELIVERED.name(), "otherUserId", userId));
                }
            }

            // For messages where the connected user is the patient
            java.util.List<String> patientSenders = chatRepository.findSendersWithSentMessagesToPatient(userId);
            if (!patientSenders.isEmpty()) {
                chatRepository.markDeliveredForRecipientPatient(userId);
                for (String senderId : patientSenders) {
                    websocketMessagingService.sendMessageStatus(userId, senderId, Map.of("status", MessageStatus.DELIVERED.name(), "otherUserId", userId));
                }
            }
        } catch (Exception e) {
            logger.error("Error while marking messages delivered on user connect for {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
}
