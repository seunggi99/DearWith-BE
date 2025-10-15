package com.dearwith.dearwith_backend.external.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Presigner s3Presigner;
    private final S3Client s3;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.region}")
    private String region;

    @Value("${app.assets.public-base-url:https://%s.s3.%s.amazonaws.com}") // CloudFront 전환시 교체
    private String publicBaseUrlPattern;


    private static final Set<String> ALLOWED_DOMAINS = Set.of("event", "review", "artist", "profile");
    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "image/webp", "image/avif");
    public record PresignOut(String url, String key, long ttlSeconds) {}

    /**
     * Presigned PUT URL 생성 (tmp/{domain}/yyyy/MM/{uuid}-{filename})
     */
    public PresignOut createPresignedPutWithKey(String domain, String filename, String contentType, Duration ttl) {
        String normalizedDomain = normalizeDomain(domain);
        String safeName = safeFilename(filename);

        String key = String.format("tmp/%s/%d/%02d/%s-%s",
                normalizedDomain,
                Year.now().getValue(),
                LocalDate.now().getMonthValue(),
                UUID.randomUUID(),
                safeName);

        if (!ALLOWED_MIME.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported contentType: " + contentType);
        }

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        var presigned = s3Presigner.presignPutObject(b -> b
                .signatureDuration(ttl)
                .putObjectRequest(objectRequest)
        );

        return new PresignOut(presigned.url().toString(), key, ttl.toSeconds());
    }
    /**
     * tmp/... → inline/... 승격 (복사 + 삭제 + 캐시 헤더 설정)
     */
    public String promoteTmpToInline(String tmpKey) {
        if (tmpKey.startsWith("inline/")) return tmpKey;
        if (!tmpKey.startsWith("tmp/")) throw new IllegalArgumentException("tmpKey must start with 'tmp/': " + tmpKey);

        HeadObjectResponse head = s3.headObject(b -> b.bucket(bucket).key(tmpKey));

        long size = head.contentLength();
        String mime = head.contentType();
        if (size <= 0 || size > 10 * 1024 * 1024L)
            throw new IllegalArgumentException("Invalid file size");
        if (!ALLOWED_MIME.contains(mime))
            throw new IllegalArgumentException("Invalid content type");

        String finalKey = tmpKey.replaceFirst("^tmp/", "inline/");

        s3.copyObject(b -> b
                .sourceBucket(bucket)
                .sourceKey(tmpKey)
                .destinationBucket(bucket)
                .destinationKey(finalKey)
                .metadataDirective(MetadataDirective.REPLACE)
                .contentType(mime)
                .cacheControl("public, max-age=31536000, immutable")
        );

        s3.deleteObject(b -> b.bucket(bucket).key(tmpKey));

        return finalKey;
    }

    /**
     * S3 또는 CloudFront 퍼블릭 URL 생성
     */
    public String generatePublicUrl(String key) {
        String encodedKey = encodePath(key);
        String base = String.format(publicBaseUrlPattern, bucket, region);
        return base.endsWith("/") ? base + encodedKey : base + "/" + encodedKey;
    }

    /** 경로 세그먼트별 안전 인코딩 */
    private String encodePath(String key) {
        String[] parts = key.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String normalizeDomain(String domain) {
        String d = Objects.requireNonNullElse(domain, "").toLowerCase(Locale.ROOT);
        if (!ALLOWED_DOMAINS.contains(d)) {
            throw new IllegalArgumentException("Unsupported domain: " + domain);
        }
        return d;
    }

    private String safeFilename(String filename) {
        String name = Objects.requireNonNullElse(filename, "file");
        name = name.replace("\\", "_").replace("/", "_");
        if (name.length() > 120) name = name.substring(name.length() - 120);
        return Normalizer.normalize(name, Normalizer.Form.NFKC);
    }
}
