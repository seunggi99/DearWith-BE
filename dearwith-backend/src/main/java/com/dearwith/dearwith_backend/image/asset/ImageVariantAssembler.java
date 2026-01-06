package com.dearwith.dearwith_backend.image.asset;

import com.dearwith.dearwith_backend.common.dto.ImageVariantDto;
import com.dearwith.dearwith_backend.image.enums.ImageProcessStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ImageVariantAssembler {

    /**
     * @param baseUrl       원본(또는 표시용) URL
     * @param profile       variant profile
     * @param processStatus 변환 상태 (READY면 variant url, 그 외는 원본 url fallback)
     */
    public List<ImageVariantDto> toVariants(String baseUrl, ImageVariantProfile profile, ImageProcessStatus processStatus) {
        if (baseUrl == null) return List.of();

        // PROCESSING/FAILED 등 READY 전에는: 기존 응답 구조(variants[])를 유지하되 url만 원본으로 통일
        if (processStatus != null && processStatus != ImageProcessStatus.READY) {
            return fallbackToOriginal(baseUrl, profile);
        }

        // null or READY: variant URL 조립
        int lastSlash = baseUrl.lastIndexOf('/');
        int lastDot   = baseUrl.lastIndexOf('.');

        if (lastSlash < 0 || lastDot < 0 || lastDot <= lastSlash) {
            return List.of(new ImageVariantDto("original", baseUrl));
        }

        String prefix = baseUrl.substring(0, lastSlash + 1);
        String stem   = baseUrl.substring(lastSlash + 1, lastDot);

        AssetVariantPreset preset = profile.preset();
        List<String> wantedCodes  = profile.codes();

        List<ImageVariantDto> result = new ArrayList<>();

        for (VariantSpec spec : preset.specs()) {
            String filename = spec.getFilename();
            String code = filename.endsWith(".webp")
                    ? filename.substring(0, filename.length() - ".webp".length())
                    : filename;

            if (!wantedCodes.contains(code)) continue;

            String url = prefix + stem + "/" + filename;

            result.add(new ImageVariantDto(code, url));
        }

        return result;
    }

    private List<ImageVariantDto> fallbackToOriginal(String baseUrl, ImageVariantProfile profile) {
        List<ImageVariantDto> result = new ArrayList<>();
        for (String code : profile.codes()) {
            result.add(new ImageVariantDto(code, baseUrl));
        }
        return result;
    }
}