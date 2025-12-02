package com.dearwith.dearwith_backend.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

    @Value("${app.firebase.admin-json-base64:}")
    private String firebaseAdminJsonBase64;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        if (firebaseAdminJsonBase64 == null || firebaseAdminJsonBase64.isBlank()) {
            throw new IllegalStateException(
                    "Firebase Admin JSON(base64)이 설정되지 않았습니다. " +
                            "환경 변수 FIREBASE_ADMIN_JSON_BASE64 또는 app.firebase.admin-json-base64를 확인하세요."
            );
        }

        byte[] decoded = Base64.getDecoder().decode(firebaseAdminJsonBase64);

        try (InputStream serviceAccount = new ByteArrayInputStream(decoded)) {
            log.info("✅ Firebase GoogleCredentials 초기화 완료");
            return GoogleCredentials.fromStream(serviceAccount)
                    .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));
        }
    }
}