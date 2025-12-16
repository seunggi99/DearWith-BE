package com.dearwith.dearwith_backend.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "rt:jti:";

    public void save(String jti, String userId, Duration ttl) {
        String key = KEY_PREFIX + jti;
        redisTemplate.opsForValue().set(key, userId, ttl);
    }

    public boolean exists(String jti) {
        String key = KEY_PREFIX + jti;
        Boolean hasKey = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(hasKey);
    }

    public void delete(String jti) {
        String key = KEY_PREFIX + jti;
        redisTemplate.delete(key);
    }
}