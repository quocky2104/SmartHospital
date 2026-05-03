package com.example.SmartHospital.config.websocket;
import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.SmartHospital.service.chat.MessageStatusService;
import com.example.SmartHospital.service.chat.OnlineStatusServicePort;

import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {
    private final OnlineStatusServicePort onlineStatusService;
    private final MessageStatusService messageStatusService;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();
        if (userPrincipal != null) {
            String userId = userPrincipal.getName(); // userId from JWT claims will be used as Principal name
            onlineStatusService.setOnline(userId);
            // Mark any pending incoming messages as delivered for this user and notify senders
            messageStatusService.handleUserConnected(userId);
        }
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();
        if (userPrincipal != null) {
            String userId = userPrincipal.getName(); // userId from JWT claims will be used as Principal name
            onlineStatusService.setOffline(userId);
        }   
    }
}
