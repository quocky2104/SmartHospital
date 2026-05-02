package com.example.SmartHospital.config.websocket;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.SmartHospital.config.jwt.JwtProvider;
import com.example.SmartHospital.service.chat.OnlineStatusServicePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketCustomChannelInterceptor implements ChannelInterceptor {
    private final JwtProvider jwtProvider;
    private final OnlineStatusServicePort onlineStatusService;
        
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // Remove "Bearer " prefix
                if (jwtProvider.validateToken(token)) {
                    String userId = jwtProvider.getUserIdFromToken(token);
                    String role = jwtProvider.getRoleFromToken(token);
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, List.of(() -> role) // Create a simple GrantedAuthority based on the role
                    );
                    accessor.setUser(authentication); // This sets Principal on the STOMP session (persisted for all subsequent frames)
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("User authenticated and set in accessor: {}", userId);
                    onlineStatusService.setOnline(userId); // Mark the user as online in Redis

                    log.info("WebSocket CONNECT received, user authenticated: {}, role: {}", userId, role);
                }
            }
        }  else if (StompCommand.DISCONNECT.equals(accessor.getCommand()) && accessor.getUser() != null) {
            onlineStatusService.setOffline(accessor.getUser().getName()); // Mark the user as offline in Redis
            String userId = accessor.getUser() != null ? accessor.getUser().getName() : "Unknown User";
            log.info("WebSocket DISCONNECT received, marking user as offline: {}", userId);   
            SecurityContextHolder.clearContext();
        }
        return message;
    }
}
