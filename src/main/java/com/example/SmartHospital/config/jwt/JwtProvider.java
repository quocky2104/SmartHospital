package com.example.SmartHospital.config.jwt;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
@Component // Enables dependency injection
@Slf4j // Enables logging for the class
public class JwtProvider {
    @Value ("${jwt.secret}")
    private String secret;

    @Value ("${jwt.access.expiration}")
    private long accessTokenExpiration;

    @Value ("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;

    @Value ("${jwt.otp.expiration}")
    private long otpExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generate access token
    public String generateAccessToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .claim("type", "ACCESS_TOKEN")
                .claim("userId", claims.get("userId"))
                .claim("email", claims.get("email"))
                .claim("roles", claims.getOrDefault("roles", List.of("ROLE_USER")))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // Generate refresh token
    public String generateRefreshToken(Map<String, Object> claims) {
        //JTI (JWT ID) is used to identify the token and to prevent token replay attacks
        return Jwts.builder()
                .claims(claims)
                .claim("type", "REFRESH_TOKEN")
                .claim("userId", claims.get("userId"))
                .claim("email", claims.get("email"))
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // Generate OTP token
    public String generateOtpToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .claim("type", "OTP")
                .claim("email", claims.get("email"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + otpExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // Validate token
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
            .verifyWith(getSigningKey()) 
            .build() 
            .parseSignedClaims(token); // Verify the token
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT");
        } catch (SecurityException e) {
            log.warn("Invalid signature");
        } catch (IllegalArgumentException e) {
            log.warn("Empty claims string");
        }
        return false;
    }


    // Get Claims
    // Structure of token:
    // header: {alg: HS256, typ: JWT}
    // payload: {username, roles, exp}
    public Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey()) 
            .build() 
            .parseSignedClaims(token).getPayload(); // Get the claims from the token
    }

    public String getType(String token) {
        return getClaims(token).get("type", String.class);
    }

    // Get authentication from token
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer "
        }
        return null;
    }
    
    public Authentication getAuthentication(String token) { 
        Claims claims = getClaims(token);  

        // Use userId claim instead of email
        String userId = claims.get("userId", String.class);
        if (userId == null) {
            throw new IllegalArgumentException("JWT token does not contain userId claim");
        }

        List<?> roles = claims.get("roles", List.class);
        if (roles == null) {
            throw new IllegalArgumentException("JWT token does not contain roles claim");
        }

        // Convert roles to Spring Security authorities
        var authorities = roles.stream()
            .map(role -> {
                String roleName = role.toString();
                if (!roleName.startsWith("ROLE_")) {
                    roleName = "ROLE_" + roleName;
                }
                return new SimpleGrantedAuthority(roleName);
            })
            .toList();

        // Now userId will be returned by userDetails.getUsername()
        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }

    // Get userId from token
    public String getUserIdFromToken(String token) {
        return getClaims(token).get("userId", String.class);
    }
    // Get role from token
    public String getRoleFromToken(String token) {
        List<?> roles = getClaims(token).get("roles", List.class);
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("JWT token does not contain roles claim");
        }
        return roles.get(0).toString(); // Assuming a user has only one role, return the first one
    }
}