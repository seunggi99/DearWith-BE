package com.dearwith.dearwith_backend.external.aws;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.image.VariantSpec;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;

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
    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/avif",
            "image/heic",
            "image/heif",
            "image/gif"
    );
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
            throw new BusinessException(ErrorCode.UNSUPPORTED_CONTENT_TYPE, "contentType=" + contentType);
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
        if (!tmpKey.startsWith("tmp/"))     throw new BusinessException(ErrorCode.INVALID_TMP_KEY, "tmpKey=" + tmpKey);

        HeadObjectResponse head = s3.headObject(b -> b.bucket(bucket).key(tmpKey));

        long size = head.contentLength();
        String mime = head.contentType();
        if (size <= 0 || size > 10 * 1024 * 1024L)
            throw new BusinessException(ErrorCode.INVALID_FILE_SIZE, "size=" + size);
        if (!ALLOWED_MIME.contains(mime))
            throw new BusinessException(ErrorCode.UNSUPPORTED_CONTENT_TYPE, "mime=" + mime);

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
     * 원본 기준으로 썸네일/미리보기/확대용 등 여러 버전 생성 & 업로드
     * - VariantSpec: filename, maxWidth, maxHeight, format(webp/jpeg), quality(0~100)
     */
    public void generateVariants(String originalKey, List<VariantSpec> specs) {
        var getReq = GetObjectRequest.builder().bucket(bucket).key(originalKey).build();

        try (var in = s3.getObject(getReq)) {
            BufferedImage src = ImageIO.read(in);
            if (src == null) throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE, "cannot read source image");

            String baseDir = getDirPrefix(originalKey);
            String stem    = getStemWithoutExt(originalKey);
            String variantDir = baseDir + stem + "/";

            for (VariantSpec spec : specs) {
                String fmt = normalizeFormat(spec.format());
                byte[] bytes;

                if (spec.maxWidth() != null && spec.maxHeight() != null && spec.maxWidth().equals(spec.maxHeight())) {
                    // 중앙 정사각 크롭 + 리사이즈
                    int size = spec.maxWidth();
                    int crop = Math.min(src.getWidth(), src.getHeight());
                    try (var baos = new ByteArrayOutputStream()) {
                        Thumbnails.of(src)
                                .sourceRegion(Positions.CENTER, crop, crop)
                                .size(Math.min(size, crop), Math.min(size, crop))
                                .outputFormat(fmt)
                                .outputQuality((spec.quality() == null ? 80 : spec.quality()) / 100.0)
                                .toOutputStream(baos);
                        bytes = baos.toByteArray();
                    }
                } else {
                    // 긴 변 기준 리사이즈 (원본보다 키우지 않음)
                    int longEdge = (spec.maxWidth() != null) ? spec.maxWidth()
                            : (spec.maxHeight() != null ? spec.maxHeight() : 2048);

                    int w = src.getWidth(), h = src.getHeight();
                    int targetLong = Math.min(longEdge, Math.max(w, h));
                    int tw = (w >= h) ? targetLong : (int)Math.round((targetLong / (double)h) * w);
                    int th = (w >= h) ? (int)Math.round((targetLong / (double)w) * h) : targetLong;

                    try (var baos = new ByteArrayOutputStream()) {
                        Thumbnails.of(src)
                                .size(Math.max(tw, 1), Math.max(th, 1))
                                .outputFormat(fmt)
                                .outputQuality((spec.quality() == null ? 80 : spec.quality()) / 100.0)
                                .toOutputStream(baos);
                        bytes = baos.toByteArray();
                    }
                }

                // ✅ 변환본을 원본-stem 하위 폴더에 저장
                String variantKey = variantDir + spec.filename();
                putBytes(variantKey, bytes, "image/" + fmt);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_PROCESSING_FAILED, e.getMessage());
        }
    }

    /** inline/.../dir/ 경로 prefix 추출 */
    public String getDirPrefix(String key) {
        int idx = key.lastIndexOf('/');
        return (idx < 0) ? "" : key.substring(0, idx + 1);
    }

    /**
     * S3 또는 CloudFront 퍼블릭 URL 생성
     */
    public String generatePublicUrl(String key) {
        String encodedKey = encodePath(key);
        String base = String.format(publicBaseUrlPattern, bucket, region);
        return base.endsWith("/") ? base + encodedKey : base + "/" + encodedKey;
    }

    private String getStemWithoutExt(String key) {
        String file = key.substring(key.lastIndexOf('/') + 1);
        int dot = file.lastIndexOf('.');
        return (dot > 0) ? file.substring(0, dot) : file;
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
            throw new BusinessException(ErrorCode.UNSUPPORTED_DOMAIN, "domain=" + domain);
        }
        return d;
    }

    private String safeFilename(String filename) {
        String name = Objects.requireNonNullElse(filename, "file");
        name = name.replace("\\", "_").replace("/", "_");
        if (name.length() > 120) name = name.substring(name.length() - 120);
        return Normalizer.normalize(name, Normalizer.Form.NFKC);
    }

    /** webp 플러그인 미존재 시 jpeg로 폴백 */
    private String normalizeFormat(String fmt) {
        if (fmt == null) return "jpeg";
        String f = fmt.toLowerCase(Locale.ROOT);
        if (f.equals("webp") && ImageIO.getImageWritersByFormatName("webp").hasNext()) return "webp";
        if (f.equals("jpg")) return "jpeg";
        if (f.equals("png") || f.equals("jpeg")) return f;
        return "jpeg";
    }

    /** S3 업로드 (SDK v2) */
    private void putBytes(String key, byte[] bytes, String contentType) {
        s3.putObject(b -> b
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .cacheControl("public, max-age=31536000, immutable"),
                RequestBody.fromBytes(bytes)
        );
    }
}
