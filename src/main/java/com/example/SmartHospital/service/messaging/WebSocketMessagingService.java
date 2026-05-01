package com.example.SmartHospital.service.messaging;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;


@Service
@RequiredArgsConstructor
@Slf4j
// Pure WebSocket messaging service, no business logic here
public class WebSocketMessagingService {
    private final SimpMessagingTemplate messagingTemplate;
    // This method can be used to send messages to the receiver
    public <T> void sendMessageToUser(String senderId, String receiverId, T response) {
        // Send the message to the receiver's private queue
        // ex: /user/{receiverId}/queue/messages
        log.info("Sending WebSocket message from user {} to user {}: {}", senderId, receiverId, response);
        messagingTemplate.convertAndSendToUser(
            receiverId, 
            "/queue/messages", 
            response
        );
    }

    // This method can be used to send message status updates (like read/delivered) to the receiver
    public <T> void sendMessageStatus(String senderId, String receiverId, T response) {
        // Send the message to the receiver's private queue
        // ex: /user/{receiverId}/queue/message-status
        log.info("Sending WebSocket message status from user {} to user {}: {}", senderId, receiverId, response);
        messagingTemplate.convertAndSendToUser(
            receiverId, 
            "/queue/message-status", 
            response
        );
    }

    // This method can be used to send typing indicators to the receiver
    public <T> void sendTypingIndicator(String senderId, String receiverId, T response) {
        // Send the typing indicator to the receiver's private queue
        // ex: /user/{receiverId}/queue/typing
        log.info("Sending WebSocket typing indicator from user {} to user {}: {}", senderId, receiverId, response);
        messagingTemplate.convertAndSendToUser(
            receiverId, 
            "/queue/typing", 
            response
        );
    }

    public <T> void sendAppointmentNotification(String senderId, String receiverId, T response) {
        log.info("Sending appointment notification from user {} to user {}: {}", senderId, receiverId, response);
        messagingTemplate.convertAndSendToUser(
            receiverId,
            "/queue/appointments",
            response
        );
    }
}
