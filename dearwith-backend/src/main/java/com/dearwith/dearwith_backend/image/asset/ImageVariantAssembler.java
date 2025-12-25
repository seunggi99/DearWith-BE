package com.dearwith.dearwith_backend.image.asset;

import com.dearwith.dearwith_backend.common.dto.ImageVariantDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ImageVariantAssembler {

    public List<ImageVariantDto> toVariants(String baseUrl, ImageVariantProfile profile) {
        if (baseUrl == null) return List.of();

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

            if (!wantedCodes.contains(code)) {
                continue;
            }

            String url = prefix + stem + "/" + filename;

            result.add(new ImageVariantDto(
                    code,
                    url
            ));
        }

        return result;
    }
}
