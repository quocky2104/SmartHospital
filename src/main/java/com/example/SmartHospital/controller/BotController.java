package com.example.SmartHospital.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.service.chat.ChatbotService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class BotController {

    private final ChatbotService chatbotService;

    @PostMapping("/bot")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<Map<String, String>> getChatbotResponse(@RequestBody Map<String, String> request, @AuthenticationPrincipal String userId) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("response", "Message cannot be empty"));
        }
        
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("response", "Unauthorized"));
        }

        String response = chatbotService.getChatbotResponse(userId, message);
        return ResponseEntity.ok(Map.of("response", response));
    }
}

