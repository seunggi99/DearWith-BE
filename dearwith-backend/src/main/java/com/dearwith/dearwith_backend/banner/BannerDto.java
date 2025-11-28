package com.dearwith.dearwith_backend.banner;

import com.dearwith.dearwith_backend.external.aws.AssetUrlService;

public record BannerDto(Long id, String imageUrl) {
    public static BannerDto of(Banner b, AssetUrlService assetUrlService) {
        return new BannerDto(
                b.getId(),
                assetUrlService.generatePublicUrl(b.getImage())
        );
    }
}
