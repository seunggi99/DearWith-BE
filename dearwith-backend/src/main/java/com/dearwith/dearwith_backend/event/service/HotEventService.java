package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.event.assembler.EventInfoAssembler;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import com.dearwith.dearwith_backend.event.entity.Event;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotEventService {

    private static final String HOT_EVENT_KEY_PREFIX = "hot:event";
    private static final String VIEWED_KEY_PREFIX      = "hot:viewed";
    private static final int DEFAULT_AGGREGATE_DAYS = 3;
    private static final Duration KEY_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final EventRepository eventRepository;
    private final EventInfoAssembler eventInfoAssembler;

    /**
     * 로그인 유저가 이벤트 상세를 조회했을 때 호출.
     * - 같은 유저가 같은 날, 같은 이벤트를 여러 번 조회해도 1번만 점수 반영.
     * - 비로그인 유저(userId == null)는 무시.
     */
    @Transactional
    public void onEventViewed(Long eventId, UUID userId) {
        if (eventId == null || userId == null) return;

        LocalDate today = LocalDate.now();
        // 예: hot:viewed:123:2025-11-19
        String viewedKey = VIEWED_KEY_PREFIX + ":" + eventId + ":" + today;

        // 해당 이벤트에 대해 오늘 이 유저가 처음 조회라면 Set에 추가됨 (added > 0)
        Long added = redisTemplate.opsForSet()
                .add(viewedKey, userId.toString());

        // 조회 기록은 짧게 유지 (예: 2일)
        redisTemplate.expire(viewedKey, Duration.ofDays(2));

        if (added != null && added > 0) {
            // 오늘 이 유저의 첫 조회인 경우에만 점수 +1
            adjustEventScore(eventId, Action.VIEW.delta());
        }
    }

    /**
     * 북마크/리뷰/좋아요 등, "플러스 액션" 발생 시 호출.
     * - 예) 북마크 추가, 리뷰 작성 등
     */
    @Transactional
    public void increaseEventScore(Long eventId, Action action) {
        if (eventId == null || action == null || action.delta() == 0L) return;
        adjustEventScore(eventId, action.delta());
    }

    /**
     * 북마크/리뷰/좋아요 등, "마이너스 액션" 발생 시 호출.
     * - 예) 북마크 취소, 리뷰 삭제 등
     */
    @Transactional
    public void decreaseEventScore(Long eventId, Action action) {
        if (eventId == null || action == null || action.delta() == 0L) return;
        adjustEventScore(eventId, -action.delta());
    }

    /**
     * 메인 페이지 등에서 상위 N개의 핫 이벤트 조회
     */
    @Transactional(readOnly = true)
    public List<EventInfoDto> getHotEvents(int limit) {
        if (limit <= 0) limit = 10;

        // 1) Redis에서 최근 N일 점수 합산
        Map<Long, Long> scoreMap = aggregateLastNDaysScores(
                HOT_EVENT_KEY_PREFIX,
                DEFAULT_AGGREGATE_DAYS
        );

        List<Event> result = new ArrayList<>();

        // 2) 핫 스코어가 있는 경우: 점수순으로 정렬 후 상위 limit 개 뽑기
        if (!scoreMap.isEmpty()) {
            List<Long> hotEventIds = scoreMap.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .toList();

            // DB에서 이벤트 조회
            List<Event> events = eventRepository.findAllById(hotEventIds);
            Map<Long, Event> eventMap = events.stream()
                    .collect(Collectors.toMap(Event::getId, e -> e));

            // 정렬 유지 + ENDED 제외
            for (Long id : hotEventIds) {
                Event e = eventMap.get(id);
                if (e == null) continue;
                if (e.getStatus() == EventStatus.ENDED) continue;
                result.add(e);
            }
        }

        // 3) 아직 limit보다 적으면: ENDED 제외 + 북마크 많은 순으로 채우기
        if (result.size() < limit) {
            int remain = limit - result.size();

            Set<Long> excludeIds = result.stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());

            List<Long> excludeIdList = excludeIds.isEmpty()
                    ? List.of(-1L) // IN () 방지용 더미
                    : new ArrayList<>(excludeIds);

            List<Event> fallback = eventRepository
                    .findTopByStatusNotAndIdNotInOrderByBookmarkCountDesc(
                            EventStatus.ENDED,
                            excludeIdList,
                            PageRequest.of(0, remain)
                    );

            result.addAll(fallback);
        }

        // 4) 최종 DTO 변환
        return result.stream()
                .map(eventInfoAssembler::assemble)
                .collect(Collectors.toList());
    }

    // ================== 내부 헬퍼 메서드 ==================

    /**
     * 오늘자 ZSET score 조정 (양수/음수 모두 처리)
     */
    private void adjustEventScore(Long eventId, long delta) {
        if (delta == 0L) return;

        String key = buildKey(HOT_EVENT_KEY_PREFIX, LocalDate.now());
        String member = String.valueOf(eventId);

        redisTemplate.opsForZSet().incrementScore(key, member, delta);
        redisTemplate.expire(key, KEY_TTL);
    }

    private String buildKey(String prefix, LocalDate date) {
        // prefix: "hot:event", key 예시: "hot:event:2025-11-18"
        return prefix + ":" + date;
    }

    private Map<Long, Long> aggregateLastNDaysScores(String prefix, int days) {
        LocalDate today = LocalDate.now();
        Map<Long, Long> scoreMap = new HashMap<>();

        for (int i = 0; i < days; i++) {
            LocalDate targetDate = today.minusDays(i);
            String key = buildKey(prefix, targetDate);

            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);

            if (tuples == null || tuples.isEmpty()) continue;

            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null || tuple.getScore() == null) continue;

                Long eventId = Long.valueOf(tuple.getValue());
                long score = tuple.getScore().longValue();

                scoreMap.merge(eventId, score, Long::sum);
            }
        }

        return scoreMap;
    }

    public enum Action {
        VIEW(1),
        BOOKMARK(5),
        REVIEW(10);
        private final int delta;
        Action(int delta) { this.delta = delta; }
        public int delta() { return delta; }
    }
}
