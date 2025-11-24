package com.dearwith.dearwith_backend.image.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.S3ClientAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageAssetService {

    private final S3ClientAdapter s3;

    @Value("${app.aws.s3.trash-prefix:trash/}")
    private String trashPrefix;

    @Value("${app.assets.cache-control:public, max-age=31536000, immutable}")
    private String cacheControl;

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg","image/png","image/webp","image/avif","image/heic","image/heif","image/gif"
    );

    /* ========= 정책 1: tmp → inline 승격 ========= */
    public String promoteTmpToInline(String tmpKey) {

        // tmpKey 검증 (FRONT 오류)
        if (tmpKey == null || tmpKey.isBlank() || !tmpKey.startsWith("tmp/")) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_TMP_KEY,
                    ErrorCode.INVALID_TMP_KEY.getMessage(),
                    "TMP_KEY_INVALID"
            );
        }

        // S3 메타 조회 (S3 NOT FOUND)
        HeadObjectResponse head;
        try {
            head = s3.head(tmpKey);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.S3_OBJECT_NOT_FOUND || e.getErrorCode() == ErrorCode.NOT_FOUND) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.S3_OBJECT_NOT_FOUND,
                        ErrorCode.S3_OBJECT_NOT_FOUND.getMessage(),
                        "TMP_OBJECT_NOT_FOUND"
                );
            }
            throw e;
        } catch (Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "TMP_HEAD_FAILED"
            );
        }

        long size = head.contentLength();
        String mime = head.contentType();

        // 파일 크기 검증 (사용자 잘못)
        if (size <= 0 || size > 10 * 1024 * 1024L) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_FILE_SIZE,
                    ErrorCode.INVALID_FILE_SIZE.getMessage(),
                    "INVALID_SIZE"
            );
        }

        // MIME 검증 (사용자 잘못)
        if (!ALLOWED_MIME.contains(mime)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.UNSUPPORTED_CONTENT_TYPE,
                    ErrorCode.UNSUPPORTED_CONTENT_TYPE.getMessage(),
                    "UNSUPPORTED_MIME"
            );
        }

        // inline key 생성
        String finalKey = tmpKey.replaceFirst("^tmp/", "inline/");

        try {
            s3.copy(tmpKey, finalKey, true, mime, cacheControl);
            s3.delete(tmpKey);
        } catch (Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "PROMOTE_TMP_FAILED"
            );
        }

        return finalKey;
    }

    /* ========= 정책 2: 원본 + 파생 디렉토리 trash 이동 ========= */
    public String moveOriginalAndDerivedToTrash(String originalKey) {

        guardFileKey(originalKey); // FRONT 오류

        if (originalKey.startsWith(trashPrefix)) {
            return originalKey;
        }

        String dstOriginal = trashPrefix + originalKey;

        // 원본 존재 여부 확인
        if (!s3.exists(originalKey)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OBJECT_NOT_FOUND,
                    ErrorCode.S3_OBJECT_NOT_FOUND.getMessage(),
                    "ORIGINAL_OBJECT_NOT_FOUND"
            );
        }

        try {
            s3.copy(originalKey, dstOriginal, false, null, null);
            s3.delete(originalKey);
        } catch (Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "ORIGINAL_MOVE_FAILED"
            );
        }

        // 파생본 처리
        String prefix = computeDerivedPrefix(originalKey);
        List<String> keys = s3.listAllKeys(prefix);

        for (String k : keys) {
            try {
                s3.copy(k, trashPrefix + k, false, null, null);
                s3.delete(k);
            } catch (Exception e) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.S3_OPERATION_FAILED,
                        ErrorCode.S3_OPERATION_FAILED.getMessage(),
                        "DERIVED_MOVE_FAILED"
                );
            }
        }

        return dstOriginal;
    }

    /* ========= 정책 3: presign (tmp 경로 생성) ========= */
    public S3ClientAdapter.PresignResult presignTmpPut(
            String domain,
            String filename,
            String contentType,
            Duration ttl
    ) {
        String d = normalizeDomain(domain); // FRONT 오류
        String safe = safeFilename(filename);

        if (!ALLOWED_MIME.contains(contentType)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.UNSUPPORTED_CONTENT_TYPE,
                    ErrorCode.UNSUPPORTED_CONTENT_TYPE.getMessage(),
                    "UNSUPPORTED_CONTENT_TYPE"
            );
        }

        String key = String.format("tmp/%s/%d/%02d/%s-%s",
                d,
                Year.now().getValue(),
                LocalDate.now().getMonthValue(),
                UUID.randomUUID(),
                safe
        );

        try {
            return s3.presignPut(key, contentType, ttl);
        } catch (Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "PRESIGN_FAILED"
            );
        }
    }

    /* ===== util ===== */
    private String computeDerivedPrefix(String originalKey) {
        int slash = originalKey.lastIndexOf('/');
        String dir = (slash >= 0) ? originalKey.substring(0, slash + 1) : "";
        String file = (slash >= 0) ? originalKey.substring(slash + 1) : originalKey;

        int dot = file.lastIndexOf('.');
        String stem = (dot >= 0) ? file.substring(0, dot) : file;

        return dir + stem + "/";
    }

    private void guardFileKey(String key) {
        if (key == null || key.isBlank() || key.endsWith("/")) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.VALIDATION_FAILED,
                    ErrorCode.VALIDATION_FAILED.getMessage(),
                    "INVALID_FILE_KEY"
            );
        }
    }

    private String normalizeDomain(String domain) {
        if (domain == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.UNSUPPORTED_DOMAIN,
                    ErrorCode.UNSUPPORTED_DOMAIN.getMessage(),
                    "DOMAIN_NULL"
            );
        }

        String d = domain.toLowerCase();
        switch (d) {
            case "event":
            case "review":
            case "artist":
            case "profile":
                return d;
            default:
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.UNSUPPORTED_DOMAIN,
                        ErrorCode.UNSUPPORTED_DOMAIN.getMessage(),
                        "UNSUPPORTED_DOMAIN=" + domain
                );
        }
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "file";
        String name = filename.replace("\\", "_").replace("/", "_");
        return (name.length() > 120) ? name.substring(name.length() - 120) : name;
    }
}