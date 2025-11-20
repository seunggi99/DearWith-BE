package com.dearwith.dearwith_backend.image.asset;

import java.util.List;


public enum ImageVariantProfile {

    // 리뷰
    REVIEW_LIST(
            AssetVariantPreset.REVIEW,
            List.of("large", "review@1x", "review@2x")
    ),

    // 포토리뷰 썸네일
    REVIEW_PHOTO(
            AssetVariantPreset.REVIEW,
            List.of("large", "photo@1x", "photo@2x")
    ),

    // 이벤트 목록 카드용
    EVENT_LIST(
            AssetVariantPreset.EVENT,
            List.of("cover@1x", "cover@2x")
    ),

    // 이벤트 상세 페이지용
    EVENT_DETAIL(
            AssetVariantPreset.EVENT,
            List.of("event@1x", "event@2x", "large")
    );

    private final AssetVariantPreset preset;
    private final List<String> codes;

    ImageVariantProfile(AssetVariantPreset preset, List<String> codes) {
        this.preset = preset;
        this.codes = codes;
    }

    public AssetVariantPreset preset() {
        return preset;
    }

    public List<String> codes() {
        return codes;
    }
}