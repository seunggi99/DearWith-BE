package com.dearwith.dearwith_backend.artist.dto;

import java.time.LocalDate;
import java.time.Instant;

public record ArtistUnifiedDto(
        Long id,
        String nameKr,
        String imageUrl,
        Type type,
        Instant createdAt,
        LocalDate birthDate,
        LocalDate debutDate
) {
    public enum Type {
        ARTIST,
        GROUP
    }
}
