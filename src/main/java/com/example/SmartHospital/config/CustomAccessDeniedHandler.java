package com.example.SmartHospital.config;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(
        HttpServletRequest request, 
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        String message = "Forbidden: You do not have permission to access " + request.getRequestURI();

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write(String.format(
                "{\"status\":403,\"message\":\"%s\",\"data\":null}",
                escapeJson(message)));
        
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
