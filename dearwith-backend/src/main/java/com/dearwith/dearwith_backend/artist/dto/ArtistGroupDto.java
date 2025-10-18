package com.dearwith.dearwith_backend.artist.dto;

public record ArtistGroupDto (
        Long id,
        String nameKr,
        String nameEn,
        String description,
        String imageUrl
) {
}
