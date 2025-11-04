package com.dearwith.dearwith_backend.image;

import java.util.List;

public final class ReviewVariantPresets {
    private ReviewVariantPresets() {
    }

    public static List<VariantSpec> reviewImageSet() {
        return List.of(
                VariantSpec.square("review@1x.webp", 160),
                VariantSpec.square("review@2x.webp", 320),
                VariantSpec.square("photo@1x.webp", 122),
                VariantSpec.square("photo@2x.webp", 244),
                VariantSpec.longEdge("large.webp", 2048)
        );
    }
}
