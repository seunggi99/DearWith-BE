package com.dearwith.dearwith_backend.common.component;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ViewCountLimiter {

    private final Cache<String, Boolean> viewLimitCache;

    /**
     * @param category   조회 대상 카테고리 (예: "EVENT_NOTICE", "SITE_NOTICE" 등)
     * @param targetId   조회 대상 ID
     * @param viewerKey  조회자 식별자 (로그인 유저: userId, 비로그인: null 또는 쿠키 등)
     * @return true  -> 이번 요청에서 viewCount를 증가시켜도 됨
     *         false -> 30분 내에 이미 증가했으므로 무시
     */
    public boolean shouldIncrease(String category, Long targetId, UUID viewerKey) {
        if (targetId == null) {
            return false;
        }

        // 비로그인
        if (viewerKey == null) {
            return false;
        }

        String key = "view:" + category + ":" + targetId + ":" + viewerKey;

        Boolean exists = viewLimitCache.getIfPresent(key);
        if (Boolean.TRUE.equals(exists)) {
            return false;
        }

        viewLimitCache.put(key, Boolean.TRUE);
        return true;
    }
}