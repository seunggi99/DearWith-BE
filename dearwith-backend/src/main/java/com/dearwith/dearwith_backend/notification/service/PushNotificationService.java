package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.common.log.BusinessLogService;
import com.dearwith.dearwith_backend.logging.constant.BusinessAction;
import com.dearwith.dearwith_backend.logging.constant.TargetType;
import com.dearwith.dearwith_backend.logging.enums.BusinessLogCategory;
import com.dearwith.dearwith_backend.notification.entity.PushDevice;
import com.dearwith.dearwith_backend.notification.repository.PushDeviceRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    private static final int DEVICE_ACTIVE_DAYS = 90;

    // ✅ FCM multicast: 최대 500 토큰/요청
    private static final int MULTICAST_MAX = 500;

    public void sendToToken(String token, String title, String body, String url) {
        if (token == null || token.isBlank()) return;
        sendToTokens(List.of(token), title, body, url);
    }

    public void sendToUser(UUID userId, String title, String body, String url) {
        if (userId == null) return;

        Instant expireThreshold = Instant.now().minus(Duration.ofDays(DEVICE_ACTIVE_DAYS));

        List<String> tokens = pushDeviceRepository.findAllByUserIdAndEnabledTrue(userId).stream()
                .filter(d -> d.getLastActiveAt() != null && d.getLastActiveAt().isAfter(expireThreshold))
                .map(PushDevice::getFcmToken)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();

        sendToTokens(tokens, title, body, url);
    }

    public void sendToUsers(List<UUID> userIds, String title, String body, String url) {
        if (userIds == null || userIds.isEmpty()) return;

        Instant expireThreshold = Instant.now().minus(Duration.ofDays(DEVICE_ACTIVE_DAYS));

        List<String> tokens = pushDeviceRepository.findAllByUserIdInAndEnabledTrue(userIds).stream()
                .filter(d -> d.getLastActiveAt() != null && d.getLastActiveAt().isAfter(expireThreshold))
                .map(PushDevice::getFcmToken)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();

        sendToTokens(tokens, title, body, url);
    }

    /**
     * ✅ 진짜 배치: 500개 단위로 MulticastMessage 발송
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

                // sendMulticast 도 가능하지만, 최신 권장 흐름은 sendEachForMulticast(응답 상세)
                BatchResponse resp = firebaseMessaging.sendEachForMulticast(message);

                totalSuccess += resp.getSuccessCount();
                totalFailure += resp.getFailureCount();

                // ✅ 실패 토큰 정리(UNREGISTERED 등): 응답별로 disable 처리
                totalDisabled += handleMulticastFailures(batch, resp, title, url);

                log.info("[push] multicast done size={} success={} fail={}",
                        batch.size(), resp.getSuccessCount(), resp.getFailureCount());

            } catch (Exception e) {
                // 배치 자체가 터진 경우(네트워크/인증 등)
                businessLogService.error(
                        BusinessLogCategory.PUSH,
                        BusinessAction.Push.PUSH_SEND_FAILED,
                        null,
                        TargetType.SYSTEM,
                        null,
                        "푸시 배치 발송 실패 (multicast 예외)",
                        Map.of(
                                "batchSize", String.valueOf(batch.size()),
                                "title", title != null ? title : "",
                                "url", url != null ? url : ""
                        ),
                        e
                );
            }
        }

        log.info("[push] multicast summary totalTokens={} success={} fail={} disabled={}",
                tokens.size(), totalSuccess, totalFailure, totalDisabled);
    }

    private MulticastMessage buildMulticastMessage(List<String> tokens, String title, String body, String url) {
        Map<String, String> data = new HashMap<>();
        data.put("title", safe(title));
        data.put("body", safe(body));
        data.put("url", safe(url));

        // notification + data 같이 내려주기
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
     * ✅ 실패 응답을 보고 토큰 비활성화(disable) + 로깅
     * - UNREGISTERED / NOT_FOUND 류는 영구 실패: disable
     * - 나머지는 disable 하지 않고 로그만 (추후 429/5xx 재시도는 4.5번에서 확장)
     */
    private int handleMulticastFailures(List<String> tokens, BatchResponse resp, String title, String url) {
        List<SendResponse> responses = resp.getResponses();
        int disabledCount = 0;

        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (r.isSuccessful()) continue;

            String token = tokens.get(i);
            Exception ex = r.getException();

            String reason = classifyDisableReason(ex);

            // 공통 실패 로깅
            businessLogService.error(
                    BusinessLogCategory.PUSH,
                    BusinessAction.Push.PUSH_SEND_FAILED,
                    null,
                    TargetType.SYSTEM,
                    null,
                    "푸시 발송 실패 (multicast 개별 실패)",
                    Map.of(
                            "tokenPrefix", tokenPrefix(token),
                            "reason", reason != null ? reason : "UNKNOWN",
                            "title", title != null ? title : "",
                            "url", url != null ? url : ""
                    ),
                    ex
            );

            // ✅ 영구 실패만 disable
            if (reason != null) {
                int affected = pushDeviceRepository.disableAllByFcmToken(token, Instant.now(), reason);
                if (affected > 0) disabledCount += affected;

                log.info("[push] token disabled. reason={} affected={} tokenPrefix={}",
                        reason, affected, tokenPrefix(token));
            }
        }

        return disabledCount;
    }

    /**
     * FirebaseMessagingException 에서 MessagingErrorCode 기반으로 disable 판단
     */
    private String classifyDisableReason(Exception ex) {
        if (!(ex instanceof FirebaseMessagingException fme)) return null;

        MessagingErrorCode code = fme.getMessagingErrorCode();
        if (code == null) return null;

        // ✅ 영구 실패: 토큰 자체가 죽음
        if (code == MessagingErrorCode.UNREGISTERED) return "UNREGISTERED";
        if (code == MessagingErrorCode.INVALID_ARGUMENT) {
            // 토큰 형식 자체가 깨진 케이스도 운영상 disable이 맞는 경우가 많음(원하면 제외 가능)
            return "INVALID_ARGUMENT";
        }

        // NOT_FOUND는 Admin SDK에서 UNREGISTERED로 귀결되는 경우가 많아서 보통 위에서 잡힘
        return null;
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    private static String tokenPrefix(String token) {
        if (token == null) return "";
        return token.length() > 10 ? token.substring(0, 10) : token;
    }

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