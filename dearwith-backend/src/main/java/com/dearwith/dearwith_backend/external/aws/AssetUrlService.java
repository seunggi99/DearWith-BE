package com.dearwith.dearwith_backend.external.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class AssetUrlService {

    @Value("${app.aws.s3.bucket}") private String bucket;
    @Value("${app.aws.region}") private String region;

    /**
     * 기본값: "https://%s.s3.%s.amazonaws.com"
     * - CloudFront 도입 시 "https://cdn.domain.com"  교체
     */
    @Value("${app.assets.public-base-url:https://%s.s3.%s.amazonaws.com}")
    private String publicBaseUrlPattern;

    public String generatePublicUrl(String key) {
        String encoded = encodePath(key);
        String base = publicBaseUrlPattern;

        if (base.contains("%s")) {
            try {
                return base.formatted(bucket, region) + (base.endsWith("/") ? "" : "/") + encoded;
            } catch (Exception ignore) {
                return base.formatted(bucket) + (base.endsWith("/") ? "" : "/") + encoded;
            }
        } else {
            return base + (base.endsWith("/") ? "" : "/") + encoded;
        }
    }

    private String encodePath(String key) {
        String[] parts = key.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
