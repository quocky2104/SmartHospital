package com.example.SmartHospital.service.chat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnlineStatusService {

    private static final String ONLINE_USERS_KEY = "online_users";
    private final StringRedisTemplate redisTemplate;

    public void setOnline(String userId) {
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
    }

    public void setOffline(String userId) {
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
    }

    public boolean isOnline(String userId) {
        Boolean member = redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId);
        return Boolean.TRUE.equals(member);
    }

    
}