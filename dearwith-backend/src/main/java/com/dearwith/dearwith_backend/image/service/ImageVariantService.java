package com.dearwith.dearwith_backend.image.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.S3ClientAdapter;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.ImageDownscaler;
import com.dearwith.dearwith_backend.image.asset.ImageOrientationNormalizer;
import com.dearwith.dearwith_backend.image.asset.VariantSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageVariantService {

    private final S3ClientAdapter s3;

    @Value("${app.assets.cache-control:public, max-age=31536000, immutable}")
    private String cacheControl;

    public void generateVariants(String originalKey, AssetVariantPreset preset) {
        if (preset == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.IMAGE_PROCESSING_FAILED,
                    ErrorCode.IMAGE_PROCESSING_FAILED.getMessage(),
                    "IMAGE_VARIANT_PRESET_NULL"
            );
        }
        generateVariants(originalKey, preset.specs(), preset.name());
    }

    private void generateVariants(String originalKey, List<VariantSpec> specs, String presetNameForLog) {
        long t0 = System.currentTimeMillis();
        log.info("[variants] start originalKey={}, preset={}, specs={}",
                originalKey,
                (presetNameForLog == null ? "-" : presetNameForLog),
                specs
        );

        long tReadStart = System.currentTimeMillis();

        byte[] originalBytes = s3.read(originalKey);

        BufferedImage src;
        try {
            // 1) 디코딩 단계에서부터 2048로 제한(서브샘플링)
            src = ImageDownscaler.readWithMaxLongEdge(originalBytes, 2048);

            long tNormalizeStart = System.currentTimeMillis();

            // 2) EXIF orientation 반영 (회전)
            src = ImageOrientationNormalizer.normalize(originalBytes, src);

            long tAfterNormalize = System.currentTimeMillis();

            // 3) GC 힌트(큰 배열 참조 해제)
            originalBytes = null;

            log.info("[variants] timing read+downscale={}ms normalize={}ms",
                    (tNormalizeStart - tReadStart),
                    (tAfterNormalize - tNormalizeStart)
            );

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[variants] failed to read source image. key={}, reason={}",
                    originalKey, e.getMessage(), e);

            throw BusinessException.withAll(
                    ErrorCode.UNSUPPORTED_IMAGE_TYPE,
                    ErrorCode.UNSUPPORTED_IMAGE_TYPE.getMessage(),
                    "IMAGE_VARIANT_SOURCE_READ_FAILED:originalKey=" + originalKey,
                    "Failed to read source image for variants. key=" + originalKey + ", reason=" + e.getMessage(),
                    e
            );
        }

        String baseDir = dirPrefix(originalKey);
        String stem = stemNoExt(originalKey);
        String variantDir = baseDir + stem + "/";

        long tUploadStart = System.currentTimeMillis();

        for (VariantSpec spec : specs) {
            try {
                String fmt = normalizeFormat(spec.getFormat());

                byte[] out = renderVariant(src, spec, fmt);
                String key = variantDir + spec.getFilename();

                String contentType = fmt.equals("jpeg") ? "image/jpeg" : "image/" + fmt;
                s3.put(key, out, contentType, cacheControl);

                log.info("[variants] uploaded key={}, size={}B", key, out.length);

                out = null;

            } catch (BusinessException e) {
                throw e;
            } catch (Exception ex) {
                log.error("[variants] failed filename={}, reason={}",
                        spec.getFilename(), ex, ex);

                throw BusinessException.withAll(
                        ErrorCode.IMAGE_PROCESSING_FAILED,
                        ErrorCode.IMAGE_PROCESSING_FAILED.getMessage(),
                        "IMAGE_VARIANT_GENERATION_FAILED:" + spec.getFilename(),
                        "Variant generation failed. originalKey=" + originalKey
                                + ", filename=" + spec.getFilename()
                                + ", reason=" + ex.getMessage(),
                        ex
                );
            }
        }

        long tEnd = System.currentTimeMillis();
        log.info("[variants] timing upload={}ms total={}ms",
                (tEnd - tUploadStart),
                (tEnd - t0)
        );
        log.info("[variants] done originalKey={} ({}ms)", originalKey, (tEnd - t0));
    }

    public String generateSingleVariant(String originalKey, AssetVariantPreset preset) {
        if (preset == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.IMAGE_PROCESSING_FAILED,
                    ErrorCode.IMAGE_PROCESSING_FAILED.getMessage(),
                    "IMAGE_VARIANT_PRESET_NULL"
            );
        }

        VariantSpec spec = preset.specs().get(0);
        long t0 = System.currentTimeMillis();
        log.info("[single-variant] start originalKey={}, spec={}", originalKey, spec);

        long tReadStart = System.currentTimeMillis();

        byte[] originalBytes = s3.read(originalKey);

        BufferedImage src;
        long tNormalizeStart;
        long tAfterNormalize;
        try {
            // 1) 디코딩 단계에서부터 2048로 제한(서브샘플링)
            src = ImageDownscaler.readWithMaxLongEdge(originalBytes, 2048);

            tNormalizeStart = System.currentTimeMillis();

            // 2) EXIF orientation 반영 (회전)
            src = ImageOrientationNormalizer.normalize(originalBytes, src);

            tAfterNormalize = System.currentTimeMillis();

            // 3) GC 힌트(큰 배열 참조 해제)
            originalBytes = null;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[single-variant] failed to read source image. key={}, reason={}",
                    originalKey, e.getMessage(), e);

            throw BusinessException.withAll(
                    ErrorCode.UNSUPPORTED_IMAGE_TYPE,
                    ErrorCode.UNSUPPORTED_IMAGE_TYPE.getMessage(),
                    "IMAGE_SINGLE_VARIANT_SOURCE_READ_FAILED:originalKey=" + originalKey,
                    "Failed to read source image for single variant. key=" + originalKey + ", reason=" + e.getMessage(),
                    e
            );
        }

        String baseDir = dirPrefix(originalKey);
        String stem = stemNoExt(originalKey);
        String variantDir = baseDir + stem + "/";

        long tUploadStart = System.currentTimeMillis();

        try {
            String fmt = normalizeFormat(spec.getFormat());

            byte[] out = renderVariant(src, spec, fmt);
            String key = variantDir + spec.getFilename();

            String contentType = fmt.equals("jpeg") ? "image/jpeg" : "image/" + fmt;
            s3.put(key, out, contentType, cacheControl);

            long tEnd = System.currentTimeMillis();
            log.info("[single-variant] timing read+downscale={}ms normalize={}ms upload={}ms total={}ms",
                    (tNormalizeStart - tReadStart),
                    (tAfterNormalize - tNormalizeStart),
                    (tEnd - tUploadStart),
                    (tEnd - t0)
            );

            log.info("[single-variant] uploaded key={}, size={}B ({}ms)",
                    key, out.length, (tEnd - t0));

            return key;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception ex) {
            log.error("[single-variant] failed filename={}, reason={}",
                    spec.getFilename(), ex, ex);

            throw BusinessException.withAll(
                    ErrorCode.IMAGE_PROCESSING_FAILED,
                    ErrorCode.IMAGE_PROCESSING_FAILED.getMessage(),
                    "IMAGE_SINGLE_VARIANT_GENERATION_FAILED:" + spec.getFilename(),
                    "Single variant generation failed. originalKey=" + originalKey
                            + ", filename=" + spec.getFilename()
                            + ", reason=" + ex.getMessage(),
                    ex
            );
        }
    }

    /* ===== 모드별 렌더 ===== */

    private byte[] renderVariant(BufferedImage src, VariantSpec spec, String fmt) throws Exception {
        Integer mw = spec.getMaxWidth();
        Integer mh = spec.getMaxHeight();

        double q = ((spec.getQuality() == null ? 80 : spec.getQuality()) / 100.0);

        if (mw != null && mh != null) {
            var mode = spec.getResizeMode();
            boolean fillCrop = (mode != null && mode.name().equalsIgnoreCase("FILL_CROP"));

            try (var baos = new ByteArrayOutputStream()) {
                var t = Thumbnails.of(src)
                        .size(mw, mh)
                        .outputFormat(fmt.equals("jpeg") ? "jpg" : fmt)
                        .outputQuality(q);

                if (fillCrop) {
                    t.crop(Positions.CENTER);
                } else {
                    t.keepAspectRatio(true);
                }

                t.toOutputStream(baos);
                return baos.toByteArray();
            }
        }

        int longEdge = (mw != null) ? mw : (mh != null ? mh : 2048);

        int w = src.getWidth(), h = src.getHeight();
        int targetLong = Math.min(longEdge, Math.max(w, h));

        int tw = (w >= h)
                ? targetLong
                : (int) Math.round((targetLong / (double) h) * w);
        int th = (w >= h)
                ? (int) Math.round((targetLong / (double) w) * h)
                : targetLong;

        try (var baos = new ByteArrayOutputStream()) {
            Thumbnails.of(src)
                    .size(Math.max(1, tw), Math.max(1, th))
                    .outputFormat(fmt.equals("jpeg") ? "jpg" : fmt)
                    .outputQuality(q)
                    .toOutputStream(baos);
            return baos.toByteArray();
        }
    }

    private String dirPrefix(String key) {
        int idx = key.lastIndexOf('/');
        return (idx < 0) ? "" : key.substring(0, idx + 1);
    }

    private String stemNoExt(String key) {
        String file = key.substring(key.lastIndexOf('/') + 1);
        int dot = file.lastIndexOf('.');
        return (dot > 0) ? file.substring(0, dot) : file;
    }

    private String normalizeFormat(String fmt) {
        if (fmt == null) return "jpeg";
        String f = fmt.toLowerCase(Locale.ROOT);

        if (f.equals("webp")) {
            String arch = System.getProperty("os.arch");
            String os   = System.getProperty("os.name");
            if (arch.contains("aarch") || os.toLowerCase().contains("mac")) {
                return "jpeg";
            }
        }

        if (f.equals("jpg")) return "jpeg";
        if (f.equals("png") || f.equals("jpeg") || f.equals("webp")) return f;
        return "jpeg";
    }
}