package com.dearwith.dearwith_backend.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String JTI_PREFIX = "rt:jti:";
    private static final String USER_PREFIX = "rt:user:";

    /* =========================
     * 저장
     * ========================= */
    public void save(String jti, String userId, Duration ttl) {
        String jtiKey = JTI_PREFIX + jti;
        String userKey = USER_PREFIX + userId;

        // jti -> userId
        redisTemplate.opsForValue().set(jtiKey, userId, ttl);

        // userId -> jti set
        redisTemplate.opsForSet().add(userKey, jti);

        Long currentTtl = redisTemplate.getExpire(userKey);
        if (currentTtl == null || currentTtl < 0 || currentTtl < ttl.getSeconds()) {
            redisTemplate.expire(userKey, ttl);
        }
    }

    /* =========================
     * 존재 여부
     * ========================= */
    public boolean exists(String jti) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(JTI_PREFIX + jti)
        );
    }

    /* =========================
     * 단일 삭제 (로그아웃)
     * ========================= */
    public void delete(String jti) {
        String jtiKey = JTI_PREFIX + jti;

        String userId = redisTemplate.opsForValue().get(jtiKey);
        if (userId != null && !userId.isBlank()) {
            String userKey = USER_PREFIX + userId;
            redisTemplate.opsForSet().remove(userKey, jti);

            Long size = redisTemplate.opsForSet().size(userKey);
            if (size == null || size <= 0) {
                redisTemplate.delete(userKey);
            }
        }

        redisTemplate.delete(jtiKey);
    }

    /* =========================
     * 전체 삭제 (탈퇴 / 강제 로그아웃)
     * ========================= */
    public void deleteAllByUserId(UUID userId) {
        String userKey = USER_PREFIX + userId;

        Set<String> jtis = redisTemplate.opsForSet().members(userKey);
        if (jtis == null || jtis.isEmpty()) {
            redisTemplate.delete(userKey);
            return;
        }

        List<String> jtiKeys = jtis.stream()
                .map(jti -> JTI_PREFIX + jti)
                .toList();

        redisTemplate.delete(jtiKeys);
        redisTemplate.delete(userKey);
    }
}