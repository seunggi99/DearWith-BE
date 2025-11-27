package com.dearwith.dearwith_backend.artist.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ArtistUnifiedDto(
        Long id,
        String nameKr,
        String nameEn,
        String imageUrl,
        Type type,
        LocalDateTime createdAt,
        LocalDate birthDate,
        LocalDate debutDate
) {
    public enum Type {
        ARTIST,
        GROUP
    }
}
