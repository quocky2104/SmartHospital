package com.example.SmartHospital.service.token;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, Object> redisTemplate; // RedisTemplate<key, value> to access Redis

    private static final String BLACKLIST_PREFIX = "blacklist:access:";
    private static final String REFRESH_PREFIX = "refresh:";

    // Save access token to blacklist
    public void blacklistToken(String token, Duration duration) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, true, duration);
    }

    // Check if access token is blacklisted
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().get(BLACKLIST_PREFIX + token));
    }

    // Save refresh token to Redis
    public void storeRefreshToken(String email, String jti, String token, Duration duration) {
        String key = REFRESH_PREFIX + email + ":" + jti;
        redisTemplate.opsForValue().set(key, token, duration);
    }

    // Check if refresh token is valid
    public boolean isValidRefreshToken(String email, String jti, String token) {
        String key = REFRESH_PREFIX + email + ":" + jti;
        Object stored = redisTemplate.opsForValue().get(key);
        return token.equals(stored);
    }

    // Delete refresh token (logout, rotation)
    public void deleteRefreshToken(String email, String jti) {
        String key = REFRESH_PREFIX + email + ":" + jti;
        redisTemplate.delete(key);
    }

    // Delete all refresh tokens (force logout all)
    public void deleteAllRefreshTokens(String email) {
        String pattern = REFRESH_PREFIX + email + ":*";
        redisTemplate.keys(pattern).forEach(redisTemplate::delete);
    }
}