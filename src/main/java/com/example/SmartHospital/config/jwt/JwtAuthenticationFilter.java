package com.example.SmartHospital.config.jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.SmartHospital.service.token.RedisTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.*;

import java.io.IOException;

@Component
@EnableWebSecurity
@RequiredArgsConstructor
// OncePerRequestFilter: Ensures the filter is executed once per request
// JwtAuthenticationFilter: Custom filter for JWT authentication
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;


    @Override 
    protected void doFilterInternal(
        //Sets the user's authentication in Spring Security's context
        @NonNull HttpServletRequest request, 
        @NonNull HttpServletResponse response, 
        @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Get the token from the request header
        String token = jwtProvider.resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate the token
            if (!jwtProvider.validateToken(token)) {
                throw new JwtException("Invalid JWT");
            }

            // Check if the token is blacklisted in Redis
            if (redisTokenService.isTokenBlacklisted(token)) {
                throw new JwtException("Token is blacklisted");
            }

            // Extracts the user's authentication details
            // Get Type of authentication
            Claims claims = jwtProvider.getClaims(token);
            String tokenType = claims.get("type", String.class);
            
            // Block OTP tokens from general API access
            if (tokenType.equals("OTP")) {
                throw new JwtException("OTP token is invalid or expired");
            } 
            // Block refresh tokens from general API access
            if (tokenType.equals("REFRESH_TOKEN")) {
               throw new JwtException("Refresh token is invalid or expired");
            } 
            // Only ACCESS_TOKEN is valid for API calls
            if (!tokenType.equals("ACCESS_TOKEN")) {
                filterChain.doFilter(request, response);
                return;
            }
            Authentication auth = jwtProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch(JwtException e) {
            // Token is invalid or expired
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response); // Pass control to the next filter
    }
}