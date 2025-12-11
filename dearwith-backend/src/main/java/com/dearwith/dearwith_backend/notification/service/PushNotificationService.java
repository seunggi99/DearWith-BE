package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.common.log.BusinessLogService;
import com.dearwith.dearwith_backend.logging.constant.BusinessAction;
import com.dearwith.dearwith_backend.logging.constant.TargetType;
import com.dearwith.dearwith_backend.logging.enums.BusinessLogCategory;
import com.dearwith.dearwith_backend.notification.entity.PushDevice;
import com.dearwith.dearwith_backend.notification.repository.PushDeviceRepository;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final GoogleCredentials googleCredentials;
    private final PushDeviceRepository pushDeviceRepository;
    private final BusinessLogService businessLogService;

    // 최근 90일 안에 사용된 기기만 대상으로 푸시 발송
    private static final int DEVICE_ACTIVE_DAYS = 90;

    /* ============================================================
     * 1. 단일 토큰 발송 (FCM HTTP v1)
     * ============================================================ */
    public void sendToToken(String token, String title, String body, String url) {
        try {
            googleCredentials.refreshIfExpired();
            String accessToken = googleCredentials.getAccessToken().getTokenValue();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://fcm.googleapis.com/v1/projects/dearwith-6898c/messages:send"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            createMessage(token, title, body, url)
                    ))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String responseBody = response.body();

            // 성공(2xx)이 아니면 실패로 간주 → 비즈니스 로그 남김
            if (status / 100 != 2) {
                businessLogService.error(
                        BusinessLogCategory.PUSH,
                        BusinessAction.Push.PUSH_SEND_FAILED,
                        null,                        // actor: 시스템 작업
                        TargetType.SYSTEM,           // 시스템 관점의 실패
                        null,
                        "푸시 발송 실패 (FCM 비정상 응답 코드)",
                        Map.of(
                                "statusCode", String.valueOf(status),
                                "responseBody", responseBody != null ? responseBody : "",
                                "tokenPrefix", tokenPrefix(token),
                                "title", title != null ? title : "",
                                "url", url != null ? url : ""
                        ),
                        null
                );
            }

            log.debug("[FCM] status={}, body={}", status, responseBody);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            businessLogService.error(
                    BusinessLogCategory.PUSH,
                    BusinessAction.Push.PUSH_SEND_FAILED,
                    null,
                    TargetType.SYSTEM,
                    null,
                    "푸시 발송 실패 (InterruptedException)",
                    Map.of(
                            "tokenPrefix", tokenPrefix(token),
                            "title", title != null ? title : "",
                            "url", url != null ? url : ""
                    ),
                    e
            );

            throw new IllegalStateException("FCM 요청이 인터럽트되었습니다.", e);
        } catch (IOException e) {

            businessLogService.error(
                    BusinessLogCategory.PUSH,
                    BusinessAction.Push.PUSH_SEND_FAILED,
                    null,
                    TargetType.SYSTEM,
                    null,
                    "푸시 발송 실패 (IO 예외)",
                    Map.of(
                            "tokenPrefix", tokenPrefix(token),
                            "title", title != null ? title : "",
                            "url", url != null ? url : ""
                    ),
                    e
            );

            throw new IllegalStateException("FCM 요청 중 IO 오류가 발생했습니다.", e);
        }
    }

    /* ============================================================
     * 2. 유저 단위 발송 (한 명)
     *  - userId 로 PushDevice 조회 + 90일 이내 기기만 발송
     * ============================================================ */
    public void sendToUser(UUID userId, String title, String body, String url) {
        LocalDateTime expireThreshold = LocalDateTime.now().minusDays(DEVICE_ACTIVE_DAYS);

        List<PushDevice> devices = pushDeviceRepository.findAllByUserId(userId).stream()
                .filter(d -> d.getLastActiveAt().isAfter(expireThreshold)) // 오래된 기기 제외
                .toList();

        devices.stream()
                .map(PushDevice::getFcmToken)
                .distinct()
                .forEach(token -> sendToToken(token, title, body, url));
    }

    /* ============================================================
     * 3. 여러 유저 발송
     *  - userIds 전체에 대해 PushDevice 조회 후 토큰만 모아서 발송
     * ============================================================ */
    public void sendToUsers(List<UUID> userIds, String title, String body, String url) {
        if (userIds == null || userIds.isEmpty()) return;

        LocalDateTime expireThreshold = LocalDateTime.now().minusDays(DEVICE_ACTIVE_DAYS);

        List<PushDevice> devices = pushDeviceRepository.findAllByUserIdIn(userIds).stream()
                .filter(d -> d.getLastActiveAt().isAfter(expireThreshold))
                .toList();

        devices.stream()
                .map(PushDevice::getFcmToken)
                .distinct()
                .forEach(token -> sendToToken(token, title, body, url));
    }

    /* ============================================================
     * 4. FCM 메시지 JSON 생성
     * ============================================================ */
    private String createMessage(String token, String title, String body, String url) {
        return """
        {
          "message": {
            "token": "%s",
            "notification": {
              "title": "%s",
              "body": "%s"
            },
            "data": {
              "title": "%s",
              "body": "%s",
              "url": "%s"
            }
          }
        }
        """.formatted(token, escape(title), escape(body), escape(title), escape(body), escape(url));
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\"", "\\\"");
    }

    private String tokenPrefix(String token) {
        if (token == null) return "";
        return token.length() > 10 ? token.substring(0, 10) : token;
    }
}