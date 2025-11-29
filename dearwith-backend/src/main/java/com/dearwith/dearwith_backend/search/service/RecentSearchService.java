package com.dearwith.dearwith_backend.search.service;

import com.dearwith.dearwith_backend.search.util.QueryNormalizer;
import com.dearwith.dearwith_backend.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecentSearchService {
    private final StringRedisTemplate redis;
    private final QueryNormalizer normalizer;
    private final UserReader userReader;

    @Value("${app.search.recent.ttl-days:7}")
    private int ttlDays;

    private String key(UUID userId) {
        return "recent:search:" + userId;
    }

    public void add(UUID userId, String rawQuery) {
        userReader.getLoginAllowedUser(userId);
        String q = normalizer.normalize(rawQuery);
        if (q.isBlank()) return;

        String key = key(userId);
        try {
            // 중복 제거
            redis.opsForList().remove(key, 0, q);
            redis.opsForList().leftPush(key, q);
            redis.opsForList().trim(key, 0, 9);
            if (ttlDays > 0) {
                redis.expire(key, Duration.ofDays(ttlDays));
            }
        } catch (Exception e) {
            // Redis 장애 시 무시(서비스 디그레이드)
        }
    }

    /** 전체 조회(최신 → 과거) */
    public List<String> list(UUID userId) {
        userReader.getLoginAllowedUser(userId);
        String key = key(userId);
        List<String> all = redis.opsForList().range(key, 0, -1);
        return all == null ? List.of() : all;
    }

    /** 특정 검색어 삭제 */
    public void remove(UUID userId, String rawQuery) {
        userReader.getLoginAllowedUser(userId);
        String q = normalizer.normalize(rawQuery);
        if (q.isBlank()) return;
        String key = key(userId);
        try {
            redis.opsForList().remove(key, 0, q);
        } catch (Exception ignored) {}
    }

    /** 전체 삭제 */
    public void clear(UUID userId) {
        userReader.getLoginAllowedUser(userId);
        try {
            redis.delete(key(userId));
        } catch (Exception ignored) {}
    }
}
