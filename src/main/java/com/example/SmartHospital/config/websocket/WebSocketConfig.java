package com.example.SmartHospital.config.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketCustomChannelInterceptor authenticationInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefixes for message destinations
        // /topic for broadcast messages (ex: doctor-12345 sends message to all patients)
        // /queue for direct messages (ex: doctor-12345 sends message to patient-67890)
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for private messages (ex: /user/queue/messages for patient-67890 to receive direct messages)
        config.setUserDestinationPrefix("/user"); 
        // Prefix for messages sent from clients to server (ex: doctor-12345 sends message to /app/chat)
        config.setApplicationDestinationPrefixes("/app"); 
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint for WebSocket connections, clients will connect to /ws
        registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*") // Allow all origins for CORS 
        // Fallback options for browsers that don’t support WebSockets (long polling or HTTP streaming)
        .withSockJS();
    }

    // Principal is a functional interface with a single method: String getName();
    // Intercept the CONNECT message to extract and validate the JWT token, then set the authenticated user in the WebSocket session
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authenticationInterceptor);
    }
}
