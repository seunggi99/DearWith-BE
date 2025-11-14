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
//“S3 원본 자산 I/O + 정책”
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
        if (tmpKey == null || tmpKey.isBlank() || !tmpKey.startsWith("tmp/")) {
            throw new BusinessException(ErrorCode.INVALID_TMP_KEY, "tmpKey=" + tmpKey);
        }
        HeadObjectResponse head = s3.head(tmpKey);
        long size = head.contentLength();
        String mime = head.contentType();

        if (size <= 0 || size > 10 * 1024 * 1024L)
            throw new BusinessException(ErrorCode.INVALID_FILE_SIZE, "size=" + size);
        if (!ALLOWED_MIME.contains(mime))
            throw new BusinessException(ErrorCode.UNSUPPORTED_CONTENT_TYPE, "mime=" + mime);

        String finalKey = tmpKey.replaceFirst("^tmp/", "inline/");
        s3.copy(tmpKey, finalKey, true, mime, cacheControl);
        s3.delete(tmpKey);
        return finalKey;
    }

    /* ========= 정책 2: 원본 + 파생 디렉토리 trash 이동 ========= */
    public String moveOriginalAndDerivedToTrash(String originalKey) {
        guardFileKey(originalKey);
        if (originalKey.startsWith(trashPrefix)) return originalKey;

        String dstOriginal = trashPrefix + originalKey;

        // 원본 이동
        if (s3.exists(originalKey)) {
            s3.copy(originalKey, dstOriginal, false, null, null);
            s3.delete(originalKey);
        } else {
            throw new BusinessException(ErrorCode.NOT_FOUND, "원본 S3 객체가 없습니다. key=" + originalKey);
        }

        // 파생(prefix) 이동
        String prefix = computeDerivedPrefix(originalKey);
        List<String> keys = s3.listAllKeys(prefix);
        for (String k : keys) {
            String dst = trashPrefix + k;
            s3.copy(k, dst, false, null, null);
            s3.delete(k);
        }
        return dstOriginal;
    }

    /* ========= 정책 3: presign (도메인 규칙 포함: tmp 경로 생성) ========= */
    public S3ClientAdapter.PresignResult presignTmpPut(String domain, String filename, String contentType, Duration ttl) {
        String d = normalizeDomain(domain);
        String safe = safeFilename(filename);
        String key = String.format("tmp/%s/%d/%02d/%s-%s",
                d, Year.now().getValue(), LocalDate.now().getMonthValue(), UUID.randomUUID(), safe);
        if (!ALLOWED_MIME.contains(contentType)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_CONTENT_TYPE, "contentType=" + contentType);
        }
        return s3.presignPut(key, contentType, ttl);
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
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "파일 키가 유효하지 않습니다. key=" + key);
        }
    }

    private String normalizeDomain(String domain) {
        if (domain == null) throw new BusinessException(ErrorCode.UNSUPPORTED_DOMAIN, "domain=null");
        String d = domain.toLowerCase();
        switch (d) {
            case "event": case "review": case "artist": case "profile":
                return d;
            default:
                throw new BusinessException(ErrorCode.UNSUPPORTED_DOMAIN, "domain=" + domain);
        }
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "file";
        String name = filename.replace("\\", "_").replace("/", "_");
        return (name.length() > 120) ? name.substring(name.length() - 120) : name;
    }
}
