package com.dearwith.dearwith_backend.artist.dto;

import java.time.LocalDate;
import java.time.Instant;

public record ArtistUnifiedResponseDto(
        Long id,
        String nameKr,
        String imageUrl,
        Type type,
        Instant createdAt,
        LocalDate birthDate,
        LocalDate debutDate,
        Boolean bookmarked
) {
    public enum Type {
        ARTIST,
        GROUP
    }
}

