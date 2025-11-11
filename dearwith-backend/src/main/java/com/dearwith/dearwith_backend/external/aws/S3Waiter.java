package com.dearwith.dearwith_backend.external.aws;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Waiter {

    private final S3Client s3Client;
    @Value("${app.aws.s3.bucket}") private String bucket;

    public void waitUntilExists(String key) {
        int maxRetry = 3;
        long sleepMs = 300L;

        for (int i = 1; i <= maxRetry; i++) {
            try {
                s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
                log.info("[s3Waiter] Object exists on S3: key={}", key);
                return;
            } catch (Exception e) {
                log.warn("[s3Waiter] Object not visible yet (try {}/{}): key={}", i, maxRetry, key);
                try {
                    Thread.sleep(sleepMs * i);
                } catch (InterruptedException ignored) {}
            }
        }

        log.error("[s3Waiter] Object still not found on S3 after {} retries: key={}", maxRetry, key);
    }
}