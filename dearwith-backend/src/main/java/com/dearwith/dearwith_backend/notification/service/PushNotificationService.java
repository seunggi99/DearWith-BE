package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.notification.entity.PushDevice;
import com.dearwith.dearwith_backend.notification.repository.PushDeviceRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.service.UserReader;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final GoogleCredentials googleCredentials;
    private final PushDeviceRepository pushDeviceRepository;
    private final UserReader userReader;

    private static final int DEVICE_ACTIVE_DAYS = 90; // 만료 기준

    /* ============================================================
     * 1. 단일 토큰 발송
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

            System.out.println("[FCM] status=" + response.statusCode() + ", body=" + response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FCM 요청이 인터럽트되었습니다.", e);
        } catch (IOException e) {
            throw new IllegalStateException("FCM 요청 중 IO 오류가 발생했습니다.", e);
        }
    }

    /* ============================================================
     * 2. 유저 단위 발송
     * ============================================================ */
    public void sendToUser(UUID userId, String title, String body, String url) {

        LocalDateTime expireThreshold = LocalDateTime.now().minusDays(DEVICE_ACTIVE_DAYS);

        List<PushDevice> devices =
                pushDeviceRepository.findAllByUserId(userId).stream()
                        .filter(d -> d.getLastActiveAt().isAfter(expireThreshold)) // 오래된 기기 제외
                        .toList();

        devices.stream()
                .map(PushDevice::getFcmToken)
                .distinct()
                .forEach(token -> sendToToken(token, title, body, url));
    }

    /* ============================================================
     * 3. 여러 유저 발송
     * ============================================================ */
    public void sendToUsers(List<UUID> userIds, String title, String body, String url) {
        if (userIds == null || userIds.isEmpty()) return;

        LocalDateTime expireThreshold = LocalDateTime.now().minusDays(DEVICE_ACTIVE_DAYS);

        List<PushDevice> devices =
                pushDeviceRepository.findAllByUserIdIn(userIds).stream()
                        .filter(d -> d.getLastActiveAt().isAfter(expireThreshold))
                        .toList();

        devices.stream()
                .map(PushDevice::getFcmToken)
                .distinct()
                .forEach(token -> sendToToken(token, title, body, url));
    }

    /* ============================================================
     * 4. 서비스 알림 (전체)
     * ============================================================ */

    public void sendServiceNoticeToUser(UUID userId, String title, String body, String url) {
        User user = userReader.getLoginAllowedUser(userId);

        if (!user.isServiceNotificationEnabled()) return;

        sendToUser(userId, title, body, url);
    }

    public void sendServiceNoticeToUsers(List<UUID> userIds, String title, String body, String url) {
        List<UUID> targets = userReader.getLoginAllowedUsers(userIds).stream()
                .filter(User::isServiceNotificationEnabled)
                .map(User::getId)
                .toList();

        sendToUsers(targets, title, body, url);
    }

    public void sendSystemNotice(String title, String body, String url) {
        List<UUID> targets = userReader.getAllLoginAllowedUsers().stream()
                .filter(User::isServiceNotificationEnabled)
                .map(User::getId)
                .toList();

        sendToUsers(targets, title, body, url);
    }

    /* ============================================================
     * 5. 이벤트 알림
     * ============================================================ */

    public void sendEventNoticeToUsers(List<UUID> userIds, String title, String body, String url) {
        List<UUID> targets = userReader.getLoginAllowedUsers(userIds).stream()
                .filter(User::isEventNotificationEnabled)
                .map(User::getId)
                .toList();

        sendToUsers(targets, title, body, url);
    }

    public void sendEventNoticeToUser(UUID userId, String title, String body, String url) {
        User user = userReader.getLoginAllowedUser(userId);

        if (!user.isEventNotificationEnabled()) return;

        sendToUser(userId, title, body, url);
    }

    /* ============================================================
     * 6. 메시지 JSON 생성
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
}