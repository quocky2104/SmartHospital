package com.example.SmartHospital.controller;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.ChatDtos.ChatConversationDTO;
import com.example.SmartHospital.dtos.ChatDtos.ChatMessageRequest;
import com.example.SmartHospital.dtos.ChatDtos.ChatMessageResponse;
import com.example.SmartHospital.service.chat.ChatService;
import com.example.SmartHospital.service.chat.MessageStatusService;
import com.example.SmartHospital.service.chat.OnlineStatusServicePort;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private static final String OTHER_USER_ID = "otherUserId";

    private final ChatService chatService;
    private final OnlineStatusServicePort onlineStatusService;
    private final MessageStatusService messageStatusService;

    @GetMapping("/getAllChats")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @Operation(
        summary = "Get all chat conversations",
        description = "Retrieve all active chat conversations for the authenticated user with most recent messages"
    )
    public ResponseEntity<List<ChatConversationDTO>> getAllChats(@AuthenticationPrincipal String userId, Authentication authentication) {
        try {
            List<ChatConversationDTO> chatHistory = chatService.getAllChats(userId, authentication);
            return ResponseEntity.ok(chatHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }    
    }
    @GetMapping("/getChatHistory")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @Operation(
        summary = "Get chat history",
        description = "Retrieve complete message history between the authenticated user and a specific other user"
    )
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(@AuthenticationPrincipal String userId, @RequestParam("otherUserId") String otherUserId, Authentication authentication) {
        try {
            List<ChatMessageResponse> chatHistory = chatService.getSpecificChat(userId, otherUserId, authentication);
            return ResponseEntity.ok(chatHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/isOnline")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @Operation(
        summary = "Check user online status",
        description = "Check if a specific user is currently online in the chat system"
    )
    public ResponseEntity<Boolean> getOnlineStatus(@RequestParam("userId") String userId) {
        try {
            boolean isOnline = onlineStatusService.isOnline(userId);
            return ResponseEntity.ok(isOnline);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @MessageMapping("/send")
    @Operation(
        summary = "Send message",
        description = "Send a real-time message to another user via WebSocket. Message is persisted and marked as sent"
    )
    public void sendMessage(ChatMessageRequest message, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to send messages");
        }
        String senderId = principal.getName(); // userId from JWT claims will be used as Principal name
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) principal;
        chatService.createMessage(senderId, message, authentication);
        
    }

    @MessageMapping("/typing")
    @Operation(
        summary = "Typing indicator",
        description = "Forward typing state to the other user's private queue"
    )
    public void typing(@Payload Map<String, Object> request, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to send typing indicators");
        }

        String senderId = principal.getName();
        Object receiverValue = request.get(OTHER_USER_ID);
        if (receiverValue == null) {
            receiverValue = request.get("receiverId");
        }
        String receiverId = receiverValue != null ? String.valueOf(receiverValue) : null;
        if (receiverId == null || receiverId.isBlank()) {
            throw new IllegalArgumentException("otherUserId or receiverId is required for typing indicators");
        }

        boolean isTyping = Boolean.parseBoolean(String.valueOf(request.getOrDefault("isTyping", false)));
        messageStatusService.sendTypingIndicator(senderId, receiverId, isTyping);
    }

    @MessageMapping("/markAsRead")
    @Operation(
        summary = "Mark messages as read",
        description = "Mark all messages in a conversation with a specific user as read by the authenticated user"
    )
    public void markAsRead(@Payload Map<String, String> request, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to mark messages as read");
        }
        messageStatusService.markAsRead(principal, request.get(OTHER_USER_ID));
    }

    @MessageMapping("/markAsDelivered")
    @Operation(
        summary = "Mark messages as delivered",
        description = "Mark all messages in a conversation with a specific user as delivered to the authenticated user"
    )
    public void markAsDelivered(@Payload Map<String, String> request, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to mark messages as delivered");
        }
        messageStatusService.markAsDelivered(principal, request.get(OTHER_USER_ID));
    }

    @MessageMapping("/markAsSent")
    @Operation(
        summary = "Mark messages as sent",
        description = "Mark all messages in a conversation with a specific user as sent by the authenticated user"
    )
    public void markAsSent(@Payload Map<String, String> request, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to mark messages as sent");
        }
        messageStatusService.markAsSent(principal, request.get(OTHER_USER_ID));
    }
}