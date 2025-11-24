package com.dearwith.dearwith_backend.external.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.IllegalFormatException;

@Service
public class AssetUrlService {

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.region}")
    private String region;

    /**
     * 기본값: "https://%s.s3.%s.amazonaws.com"
     * - CloudFront 도입 시 "https://cdn.domain.com" 등으로 교체
     */
    @Value("${app.assets.public-base-url:https://%s.s3.%s.amazonaws.com}")
    private String publicBaseUrlPattern;

    public String generatePublicUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        String normalizedKey = stripLeadingSlash(key);
        String encoded = encodePath(normalizedKey);
        String base = publicBaseUrlPattern;

        if (base.contains("%s")) {
            try {
                return appendPath(base.formatted(bucket, region), encoded);
            } catch (IllegalFormatException ex) {
                return appendPath(base.formatted(bucket), encoded);
            }
        } else {
            return appendPath(base, encoded);
        }
    }

    private String stripLeadingSlash(String key) {
        if (key.startsWith("/")) {
            return key.substring(1);
        }
        return key;
    }

    private String appendPath(String base, String encodedPath) {
        if (base.endsWith("/")) {
            return base + encodedPath;
        }
        return base + "/" + encodedPath;
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