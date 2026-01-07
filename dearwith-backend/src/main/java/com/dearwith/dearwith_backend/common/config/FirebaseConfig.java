package com.dearwith.dearwith_backend.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

    @Value("${app.firebase.admin-json-base64:}")
    private String firebaseAdminJsonBase64;

    /** Firebase Admin SDK 초기화 */
    @Bean
    public FirebaseApp firebaseApp() {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(loadCredentials())
                .build();

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp already initialized");
            return FirebaseApp.getInstance();
        }

        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("FirebaseApp initialized");
        return app;
    }

    /** 멀티캐스트 발송용 */
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        log.info("FirebaseMessaging bean created");
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    /* ----------------- private ----------------- */

    private GoogleCredentials loadCredentials() {
        if (firebaseAdminJsonBase64 == null || firebaseAdminJsonBase64.isBlank()) {
            throw new IllegalStateException(
                    "Firebase Admin JSON(base64)이 설정되지 않았습니다. " +
                            "환경 변수 FIREBASE_ADMIN_JSON_BASE64 또는 app.firebase.admin-json-base64를 확인하세요."
            );
        }

        byte[] decoded = Base64.getDecoder().decode(firebaseAdminJsonBase64);

        try (InputStream serviceAccount = new ByteArrayInputStream(decoded)) {
            return GoogleCredentials.fromStream(serviceAccount);
        } catch (Exception e) {
            throw new IllegalStateException("Firebase credentials load failed", e);
        }
    }
}