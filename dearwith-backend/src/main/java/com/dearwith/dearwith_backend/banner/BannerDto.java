package com.dearwith.dearwith_backend.banner;

public record BannerDto(Long id, String imageUrl) {
    public static BannerDto of(Banner b) {
        return new BannerDto(b.getId(), b.getImage().getImageUrl());
    }
}
