package com.dearwith.dearwith_backend.image.asset;


import java.util.List;

public enum AssetVariantPreset {

    REVIEW(VariantPresetSet.builder()
            .spec(VariantSpec.builder().filename("review@1x.webp").maxWidth(160).maxHeight(160).format("webp").quality(80).build())
            .spec(VariantSpec.builder().filename("review@2x.webp").maxWidth(320).maxHeight(320).format("webp").quality(80).build())
            .spec(VariantSpec.builder().filename("photo@1x.webp").maxWidth(122).maxHeight(122).format("webp").quality(80).build())
            .spec(VariantSpec.builder().filename("photo@2x.webp").maxWidth(244).maxHeight(244).format("webp").quality(80).build())
            .spec(VariantSpec.builder().filename("large.webp").maxWidth(2048).maxHeight(null).format("webp").quality(80).build())
            .build()
    ),

    EVENT(VariantPresetSet.builder()
            .spec(VariantSpec.builder().filename("cover@1x.webp").maxWidth(180).maxHeight(252).format("webp").quality(80).build())
            .spec(VariantSpec.builder().filename("cover@2x.webp").maxWidth(360).maxHeight(504).format("webp").quality(80).build())
            .spec(VariantSpec.builder().filename("event@1x.webp").maxWidth(375).maxHeight(536).format("webp").quality(80).build())
            .spec(VariantSpec.builder().filename("event@2x.webp").maxWidth(750).maxHeight(1072).format("webp").quality(80).build())
            .spec(VariantSpec.builder().filename("large.webp").maxWidth(2048).maxHeight(null).format("webp").quality(80).build())
            .build()
    ),

    ARTIST(VariantPresetSet.builder()
            .spec(VariantSpec.builder().filename("artist@1x.webp").maxWidth(128).maxHeight(128).format("webp").quality(80).build())
            .build()
    ),

    USER(VariantPresetSet.builder()
            .spec(VariantSpec.builder().filename("user@1x.webp").maxWidth(128).maxHeight(128).format("webp").quality(80).build())
            .build()
    );

    private final VariantPresetSet set;

    AssetVariantPreset(VariantPresetSet set) {
        this.set = set;
    }

    public VariantPresetSet set() {
        return set;
    }

    public List<VariantSpec> specs() {
        return set.getSpecs();
    }
}