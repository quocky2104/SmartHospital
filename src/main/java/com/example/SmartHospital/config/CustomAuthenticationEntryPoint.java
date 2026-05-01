package com.example.SmartHospital.config;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component 
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
        HttpServletRequest request, 
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        String authError = (String) request.getAttribute("AUTH_ERROR");
        String message = authError == null || authError.isBlank()
                ? "Unauthorized: Authentication required. Please login first"
                : "Unauthorized: " + authError;

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(String.format(
                "{\"status\":401,\"message\":\"%s\",\"data\":null}",
                escapeJson(message)));
        
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
