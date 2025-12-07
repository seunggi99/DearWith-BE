package com.dearwith.dearwith_backend.artist.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ArtistUnifiedResponseDto(
        Long id,
        String nameKr,
        String imageUrl,
        Type type,
        LocalDateTime createdAt,
        LocalDate birthDate,
        LocalDate debutDate,
        Boolean bookmarked
) {
    public enum Type {
        ARTIST,
        GROUP
    }
}

