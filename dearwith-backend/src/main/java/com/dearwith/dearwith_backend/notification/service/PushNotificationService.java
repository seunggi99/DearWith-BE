package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.common.log.BusinessLogService;
import com.dearwith.dearwith_backend.logging.constant.BusinessAction;
import com.dearwith.dearwith_backend.logging.constant.TargetType;
import com.dearwith.dearwith_backend.logging.enums.BusinessLogCategory;
import com.dearwith.dearwith_backend.notification.dto.FailureDecision;
import com.dearwith.dearwith_backend.notification.dto.PushRetryJob;
import com.dearwith.dearwith_backend.notification.repository.PushDeviceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final PushDeviceRepository pushDeviceRepository;
    private final BusinessLogService businessLogService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final int DEVICE_ACTIVE_DAYS = 90;
    private static final int MULTICAST_MAX = 500;
    private static final int RETRY_MAX_ATTEMPT = 3;
    private static final String RETRY_ZSET_KEY = "push:retry:zset";

    /**
     * 단일 토큰에 푸시 전송
     */
    public void sendToToken(String token, String title, String body, String url) {
        if (token == null || token.isBlank()) return;
        sendToTokens(List.of(token), title, body, url);
    }

    /**
     * 특정 유저의 모든 활성 기기에 푸시 전송
     */
    public void sendToUser(UUID userId, String title, String body, String url) {
        if (userId == null) return;

        Instant expireThreshold = Instant.now().minus(Duration.ofDays(DEVICE_ACTIVE_DAYS));

        List<String> tokens = pushDeviceRepository.findActiveTokensByUserId(userId, expireThreshold);

        sendToTokens(tokens, title, body, url);
    }

    /**
     * 여러 유저의 모든 활성 기기에 푸시 전송
     */
    public void sendToUsers(List<UUID> userIds, String title, String body, String url) {
        if (userIds == null || userIds.isEmpty()) return;

        Instant expireThreshold = Instant.now().minus(Duration.ofDays(DEVICE_ACTIVE_DAYS));

        List<String> tokens = pushDeviceRepository.findActiveTokensByUserIds(userIds, expireThreshold);

        sendToTokens(tokens, title, body, url);
    }

    // ============================================================
    // Core Logic
    // ============================================================

    /**
     * 토큰 목록에 대한 배치 전송 (500개 단위로 chunking)
     */
    private void sendToTokens(List<String> tokens, String title, String body, String url) {
        if (tokens == null || tokens.isEmpty()) return;

        List<List<String>> chunks = chunk(tokens, MULTICAST_MAX);

        int totalSuccess = 0;
        int totalFailure = 0;
        int totalDisabled = 0;

        for (List<String> batch : chunks) {
            try {
                MulticastMessage message = buildMulticastMessage(batch, title, body, url);
                BatchResponse resp = firebaseMessaging.sendEachForMulticast(message);

                totalSuccess += resp.getSuccessCount();
                totalFailure += resp.getFailureCount();

                // 개별 실패 처리 (disable + retry)
                totalDisabled += handleMulticastFailures(batch, resp, title, body, url);

                log.info("[push] multicast batch done size={} success={} fail={}",
                        batch.size(), resp.getSuccessCount(), resp.getFailureCount());

            } catch (Exception e) {
                // 배치 전체 실패 → 재시도 큐로
                businessLogService.error(
                        BusinessLogCategory.PUSH,
                        BusinessAction.Push.PUSH_SEND_FAILED,
                        null,
                        TargetType.SYSTEM,
                        null,
                        "푸시 배치 발송 실패 (multicast 예외)",
                        Map.of(
                                "batchSize", String.valueOf(batch.size()),
                                "title", safe(title),
                                "body", safe(body),
                                "url", safe(url)
                        ),
                        e
                );

                // 배치 전체를 재시도 큐에 추가
                enqueueRetry(batch, title, body, url, 1);
            }
        }

        log.info("[push] multicast summary totalTokens={} success={} fail={} disabled={}",
                tokens.size(), totalSuccess, totalFailure, totalDisabled);
    }

    /**
     * Multicast 응답에서 개별 실패 처리
     * - 영구 실패: 토큰 disable
     * - 일시 실패: 재시도 큐에 추가
     * - 무시: 로깅만
     *
     * @return 비활성화된 토큰 수
     */
    private int handleMulticastFailures(
            List<String> tokens,
            BatchResponse resp,
            String title,
            String body,
            String url
    ) {
        Instant now = Instant.now();
        List<SendResponse> responses = resp.getResponses();

        int disabledCount = 0;
        List<String> retryTokens = new ArrayList<>();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (r.isSuccessful()) continue;

            String token = tokens.get(i);
            Exception ex = r.getException();

            // 실패 분류: disable / retry / ignore
            FailureDecision decision = classifyFailure(ex);

            // 공통 로깅
            businessLogService.error(
                    BusinessLogCategory.PUSH,
                    BusinessAction.Push.PUSH_SEND_FAILED,
                    null,
                    TargetType.SYSTEM,
                    null,
                    "푸시 발송 실패 (multicast 개별 실패)",
                    Map.of(
                            "tokenPrefix", tokenPrefix(token),
                            "shouldDisable", String.valueOf(decision.shouldDisable()),
                            "disableReason", safe(decision.disableReason()),
                            "shouldRetry", String.valueOf(decision.shouldRetry()),
                            "retryReason", safe(decision.retryReason()),
                            "title", safe(title),
                            "body", safe(body),
                            "url", safe(url)
                    ),
                    ex
            );

            // 영구 실패 → disable
            if (decision.shouldDisable()) {
                int affected = pushDeviceRepository.disableAllByFcmToken(
                        token, now, decision.disableReason()
                );
                if (affected > 0) {
                    disabledCount += affected;
                    log.info("[push] token disabled. reason={} affected={} tokenPrefix={}",
                            decision.disableReason(), affected, tokenPrefix(token));
                }
                continue;
            }

            // 일시 실패 → retry 목록에 추가
            if (decision.shouldRetry()) {
                retryTokens.add(token);
            }
            // shouldRetry=false, shouldDisable=false인 경우는 로깅만 하고 무시
        }

        // 일시 실패 토큰들을 재시도 큐로
        if (!retryTokens.isEmpty()) {
            enqueueRetry(retryTokens, title, body, url, 1);
            log.info("[push] enqueued retry tokens={}", retryTokens.size());
        }

        return disabledCount;
    }

    /**
     * FCM 실패를 영구/일시/무시로 분류
     *
     * @param ex FCM 전송 실패 예외
     * @return 실패 분류 결과
     */
    private FailureDecision classifyFailure(Exception ex) {
        if (!(ex instanceof FirebaseMessagingException fme)) {
            // SDK 외부 예외 (네트워크 등) → 재시도
            return FailureDecision.retry("NON_FCM_EXCEPTION");
        }

        MessagingErrorCode code = fme.getMessagingErrorCode();
        if (code == null) {
            return FailureDecision.retry("UNKNOWN_FCM_CODE");
        }

        // 영구 실패: 토큰이 죽음 → disable
        return switch (code) {
            case UNREGISTERED -> FailureDecision.disable("UNREGISTERED");
            case INVALID_ARGUMENT -> FailureDecision.disable("INVALID_ARGUMENT");
            case SENDER_ID_MISMATCH -> FailureDecision.disable("SENDER_ID_MISMATCH");
            case THIRD_PARTY_AUTH_ERROR -> FailureDecision.disable("AUTH_ERROR");

            // 일시 실패: 재시도 가능
            case UNAVAILABLE -> FailureDecision.retry("UNAVAILABLE");
            case INTERNAL -> FailureDecision.retry("INTERNAL");
            case QUOTA_EXCEEDED -> FailureDecision.retry("QUOTA_EXCEEDED");

            // 기타: 재시도하지 않고 무시 (로깅만)
            default -> FailureDecision.ignore("NON_RETRYABLE_" + code.name());
        };
    }

    // ============================================================
    // Retry Queue (Redis ZSET)
    // ============================================================

    /**
     * 재시도 큐에 추가 (지연 실행)
     * - 지수 백오프: 2^attempt 분 (최대 30분)
     *
     * @param tokens 재시도할 토큰 목록
     * @param title 푸시 제목
     * @param body 푸시 본문
     * @param url 딥링크 URL
     * @param attempt 현재 시도 횟수
     */
    private void enqueueRetry(
            List<String> tokens,
            String title,
            String body,
            String url,
            int attempt
    ) {
        if (attempt > RETRY_MAX_ATTEMPT) {
            log.warn("[push] max retry exceeded. attempt={} tokens={}", attempt, tokens.size());

            // 최대 재시도 초과 로깅
            businessLogService.warn(
                    BusinessLogCategory.PUSH,
                    BusinessAction.Push.PUSH_SEND_FAILED,
                    null,
                    TargetType.SYSTEM,
                    null,
                    "푸시 재시도 최대 횟수 초과",
                    Map.of(
                            "attempt", String.valueOf(attempt),
                            "tokens", String.valueOf(tokens.size()),
                            "title", safe(title),
                            "url", safe(url)
                    )
            );
            return;
        }

        if (tokens == null || tokens.isEmpty()) return;

        try {
            PushRetryJob job = new PushRetryJob(tokens, safe(title), safe(body), safe(url), attempt);
            String payload = objectMapper.writeValueAsString(job);

            // 지수 백오프: 2^attempt 분 (최대 30분)
            long delayMinutes = Math.min((long) Math.pow(2, attempt), 30);
            long runAtEpochMs = Instant.now().plus(Duration.ofMinutes(delayMinutes)).toEpochMilli();

            redisTemplate.opsForZSet().add(RETRY_ZSET_KEY, payload, runAtEpochMs);

            log.info("[push] enqueued retry. attempt={} tokens={} delayMinutes={}",
                    attempt, tokens.size(), delayMinutes);

        } catch (JsonProcessingException e) {
            log.error("[push] enqueueRetry serialization failed", e);

            businessLogService.error(
                    BusinessLogCategory.PUSH,
                    BusinessAction.Push.PUSH_SEND_FAILED,
                    null,
                    TargetType.SYSTEM,
                    null,
                    "푸시 재시도 큐 추가 실패 (직렬화 오류)",
                    Map.of(
                            "attempt", String.valueOf(attempt),
                            "tokens", String.valueOf(tokens.size())
                    ),
                    e
            );
        }
    }

    /**
     * 재시도 큐 처리 (스케줄러)
     * - 10초마다 실행
     * - 실행 시간이 된 작업을 최대 10개까지 처리
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processRetryQueue() {
        long now = Instant.now().toEpochMilli();

        // 실행 시간이 된 작업 10개만 조회
        Set<String> duePayloads = redisTemplate.opsForZSet()
                .rangeByScore(RETRY_ZSET_KEY, 0, now, 0, 10);

        if (duePayloads == null || duePayloads.isEmpty()) return;

        log.info("[push] processing retry queue size={}", duePayloads.size());

        for (String payload : duePayloads) {
            // 큐에서 제거
            redisTemplate.opsForZSet().remove(RETRY_ZSET_KEY, payload);

            try {
                PushRetryJob job = objectMapper.readValue(payload, PushRetryJob.class);

                log.info("[push] retry attempt={} tokens={}", job.attempt(), job.tokens().size());

                // 재시도는 다시 sendToTokens로
                // - 성공하면 완료
                // - 실패하면 handleMulticastFailures에서 attempt+1로 다시 큐잉
                sendToTokens(job.tokens(), job.title(), job.body(), job.url());

            } catch (JsonProcessingException e) {
                log.error("[push] processRetryQueue deserialization failed for payload={}", payload, e);

                businessLogService.error(
                        BusinessLogCategory.PUSH,
                        BusinessAction.Push.PUSH_SEND_FAILED,
                        null,
                        TargetType.SYSTEM,
                        null,
                        "푸시 재시도 큐 처리 실패 (역직렬화 오류)",
                        Map.of("payloadLength", String.valueOf(payload.length())),
                        e
                );
            } catch (Exception e) {
                log.error("[push] processRetryQueue failed for payload={}", payload, e);

                businessLogService.error(
                        BusinessLogCategory.PUSH,
                        BusinessAction.Push.PUSH_SEND_FAILED,
                        null,
                        TargetType.SYSTEM,
                        null,
                        "푸시 재시도 큐 처리 실패",
                        Map.of("payloadLength", String.valueOf(payload.length())),
                        e
                );
            }
        }
    }

    // ============================================================
    // Utilities
    // ============================================================

    /**
     * MulticastMessage 생성
     */
    private MulticastMessage buildMulticastMessage(
            List<String> tokens,
            String title,
            String body,
            String url
    ) {
        Map<String, String> data = new HashMap<>();
        data.put("title", safe(title));
        data.put("body", safe(body));
        data.put("url", safe(url));

        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(safe(title))
                        .setBody(safe(body))
                        .build())
                .putAllData(data)
                .build();
    }

    /**
     * null-safe 문자열 변환
     */
    private static String safe(String v) {
        return v == null ? "" : v;
    }

    /**
     * 토큰의 앞 10자만 반환 (로깅용)
     */
    private static String tokenPrefix(String token) {
        if (token == null) return "";
        return token.length() > 10 ? token.substring(0, 10) : token;
    }

    /**
     * 리스트를 지정된 크기로 분할
     */
    private static <T> List<List<T>> chunk(List<T> list, int size) {
        if (list == null || list.isEmpty()) return List.of();
        if (size <= 0) throw new IllegalArgumentException("size must be positive");

        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            out.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return out;
    }
}
