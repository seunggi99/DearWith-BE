package com.dearwith.dearwith_backend.image;

public record VariantSpec(
        String filename,
        Integer maxWidth,
        Integer maxHeight,
        String format,
        Integer quality
) {
    public static VariantSpec square(String name, int px) {
        return new VariantSpec(name, px, px, "webp", 80);
    }
    public static VariantSpec longEdge(String name, int px) {
        return new VariantSpec(name, px, null, "webp", 80);
    }
}
