package com.dearwith.dearwith_backend.image.asset;


import com.dearwith.dearwith_backend.image.enums.ResizeMode;

import java.util.List;

public enum AssetVariantPreset {

    REVIEW(VariantPresetSet.builder()
            // 리뷰 썸네일 (정사각) → FILL_CROP
            .spec(VariantSpec.builder()
                    .filename("review@1x.webp")
                    .maxWidth(160).maxHeight(160)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FILL_CROP)
                    .build()
            )
            .spec(VariantSpec.builder()
                    .filename("review@2x.webp")
                    .maxWidth(320).maxHeight(320)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FILL_CROP)
                    .build()
            )

            // 포토 리뷰 썸네일 (그리드) → FILL_CROP
            .spec(VariantSpec.builder()
                    .filename("photo@1x.webp")
                    .maxWidth(122).maxHeight(122)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FILL_CROP)
                    .build()
            )
            .spec(VariantSpec.builder()
                    .filename("photo@2x.webp")
                    .maxWidth(244).maxHeight(244)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FILL_CROP)
                    .build()
            )

            // 확대 / 전체보기 → 원본 비율 유지
            .spec(VariantSpec.builder()
                    .filename("large.webp")
                    .maxWidth(2048).maxHeight(null)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FIT)
                    .build()
            )
            .build()
    ),

    EVENT(VariantPresetSet.builder()
            // 카드 커버 → FILL_CROP
            .spec(VariantSpec.builder()
                    .filename("cover@1x.webp")
                    .maxWidth(180).maxHeight(252)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FILL_CROP)
                    .build()
            )
            .spec(VariantSpec.builder()
                    .filename("cover@2x.webp")
                    .maxWidth(360).maxHeight(504)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FILL_CROP)
                    .build()
            )

            // 상세 페이지 → 원본 비율 유지 리사이징
            .spec(VariantSpec.builder()
                    .filename("event@1x.webp")
                    .maxWidth(375).maxHeight(536)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FIT)
                    .build()
            )
            .spec(VariantSpec.builder()
                    .filename("event@2x.webp")
                    .maxWidth(750).maxHeight(1072)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FIT)
                    .build()
            )

            // 확대 / 원본 대체
            .spec(VariantSpec.builder()
                    .filename("large.webp")
                    .maxWidth(2048).maxHeight(null)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FIT)
                    .build()
            )
            .build()
    ),

    ARTIST(VariantPresetSet.builder()
            // 아티스트 → FILL_CROP
            .spec(VariantSpec.builder()
                    .filename("artist@1x.webp")
                    .maxWidth(128).maxHeight(128)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FILL_CROP)
                    .build()
            )
            .build()
    ),

    USER(VariantPresetSet.builder()
            // 유저 → FILL_CROP
            .spec(VariantSpec.builder()
                    .filename("user@1x.webp")
                    .maxWidth(128).maxHeight(128)
                    .format("webp").quality(80)
                    .resizeMode(ResizeMode.FILL_CROP)
                    .build()
            )
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