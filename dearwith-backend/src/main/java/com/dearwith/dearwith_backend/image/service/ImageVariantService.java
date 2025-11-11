package com.dearwith.dearwith_backend.image.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.S3ClientAdapter;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.VariantSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
            throw new IllegalArgumentException("preset must not be null");
        }
        generateVariants(originalKey, preset.specs(), preset.name());
    }

    private void generateVariants(String originalKey, List<VariantSpec> specs, String presetNameForLog) {
        long t0 = System.currentTimeMillis();
        log.info("[variants] start originalKey={}, preset={}, specs={}",
                originalKey, (presetNameForLog == null ? "-" : presetNameForLog), specs);

        byte[] originalBytes = s3.read(originalKey);
        BufferedImage src;
        try {
            src = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (src == null) throw new IllegalStateException("null image");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE, "원본 이미지를 읽을 수 없습니다.");
        }

        String baseDir = dirPrefix(originalKey);
        String stem    = stemNoExt(originalKey);
        String variantDir = baseDir + stem + "/";

        for (VariantSpec spec : specs) {
            try {
                String fmt = normalizeFormat(spec.format());
                int maxW = spec.maxWidth()  == null ? Integer.MAX_VALUE : spec.maxWidth();
                int maxH = spec.maxHeight() == null ? Integer.MAX_VALUE : spec.maxHeight();

                byte[] out = resizeOrContain(src, spec, fmt);
                String key = variantDir + spec.filename();

                String contentType = "image/" + (fmt.equals("jpg") ? "jpeg" : fmt);
                s3.put(key, out, contentType, cacheControl);

                log.info("[variants] uploaded key={}, size={}B", key, out.length);
            } catch (Exception ex) {
                log.error("[variants] failed filename={}, reason={}", spec.filename(), ex.toString(), ex);
                throw new BusinessException(ErrorCode.IMAGE_PROCESSING_FAILED, "variant 생성 실패: " + spec.filename());
            }
        }

        log.info("[variants] done originalKey={} ({}ms)", originalKey, System.currentTimeMillis() - t0);
    }

    /* ===== helpers ===== */

    private byte[] resizeOrContain(BufferedImage src, VariantSpec spec, String fmt) throws Exception {
        if (spec.maxWidth() != null && spec.maxHeight() != null) {
            int targetW = spec.maxWidth();
            int targetH = spec.maxHeight();
            int w = src.getWidth(), h = src.getHeight();
            double scale = Math.min(targetW / (double) w, targetH / (double) h);
            int rw = Math.max(1, (int) Math.round(w * scale));
            int rh = Math.max(1, (int) Math.round(h * scale));

            // 1) 비율유지 리사이즈 (중간 PNG)
            BufferedImage resized;
            try (var baos = new ByteArrayOutputStream()) {
                Thumbnails.of(src).size(rw, rh).outputFormat("png").toOutputStream(baos);
                resized = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
            }

            // 2) 캔버스 합성
            boolean transparent = fmt.equals("png") || fmt.equals("webp");
            int type = transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage canvas = new BufferedImage(targetW, targetH, type);
            var g = canvas.createGraphics();
            try {
                if (transparent) {
                    g.setComposite(java.awt.AlphaComposite.Src);
                    g.setColor(new java.awt.Color(0, 0, 0, 0));
                } else {
                    g.setColor(java.awt.Color.WHITE);
                }
                g.fillRect(0, 0, targetW, targetH);
                int x = (targetW - rw) / 2, y = (targetH - rh) / 2;
                g.drawImage(resized, x, y, null);
            } finally {
                g.dispose();
            }
            try (var baos = new ByteArrayOutputStream()) {
                ImageIO.write(canvas, fmt.equals("jpeg") ? "jpg" : fmt, baos);
                return baos.toByteArray();
            }
        } else {
            int longEdge = (spec.maxWidth() != null) ? spec.maxWidth()
                    : (spec.maxHeight() != null ? spec.maxHeight() : 2048);
            int w = src.getWidth(), h = src.getHeight();
            int targetLong = Math.min(longEdge, Math.max(w, h));
            int tw = (w >= h) ? targetLong : (int) Math.round((targetLong / (double) h) * w);
            int th = (w >= h) ? (int) Math.round((targetLong / (double) w) * h) : targetLong;

            try (var baos = new ByteArrayOutputStream()) {
                Thumbnails.of(src)
                        .size(Math.max(1, tw), Math.max(1, th))
                        .outputFormat(fmt)
                        .outputQuality((spec.quality() == null ? 80 : spec.quality()) / 100.0)
                        .toOutputStream(baos);
                return baos.toByteArray();
            }
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
