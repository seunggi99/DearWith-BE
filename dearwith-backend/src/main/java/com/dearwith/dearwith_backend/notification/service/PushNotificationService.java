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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final GoogleCredentials googleCredentials;
    private final PushDeviceRepository pushDeviceRepository;
    private final UserReader userReader;

    public void sendToToken(String token, String title, String body, String url){

        try {
            // 1) access token 만들기
            googleCredentials.refreshIfExpired();
            String accessToken = googleCredentials.getAccessToken().getTokenValue();

            // 2) Firebase HTTP 요청 보내기
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://fcm.googleapis.com/v1/projects/dearwith-6898c/messages:send"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            createMessage(token, title, body, url)
                    ))
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            // 필요하면 응답 로그
            System.out.println("[FCM] status=" + response.statusCode() + ", body=" + response.body());

        } catch (InterruptedException e) {
            // 인터럽트 플래그 다시 세워주고 런타임 예외로 래핑
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FCM 요청이 인터럽트되었습니다.", e);
        } catch (IOException e) {
            throw new IllegalStateException("FCM 요청 중 IO 오류가 발생했습니다.", e);
        }
    }

    public void sendToUser(UUID userId, String title, String body, String url) {
        List<PushDevice> devices = pushDeviceRepository.findAllByUserId(userId);

        for (PushDevice device : devices) {
            if (!device.isActive()) continue;
            sendToToken(device.getFcmToken(), title, body, url);
        }
    }

    public void sendServiceNoticeToUser(UUID userId, String title, String body, String url) {
        User user = userReader.getLoginAllowedUser(userId);

        if (!user.isServiceNotificationEnabled()) {
            return;
        }

        sendToUser(userId, title, body, url);
    }

    public void sendEventNoticeToUser(UUID userId, String title, String body, String url) {
        User user = userReader.getLoginAllowedUser(userId);

        if (!user.isEventNotificationEnabled()) {
            return;
        }

        sendToUser(userId, title, body, url);
    }

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