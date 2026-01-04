package com.dearwith.dearwith_backend.artist.dto;

import com.dearwith.dearwith_backend.artist.enums.ArtistType;

import java.time.LocalDate;
import java.time.Instant;

public record ArtistUnifiedDto(
        Long id,
        String nameKr,
        String imageUrl,
        ArtistType type,
        Instant createdAt,
        LocalDate birthDate,
        LocalDate debutDate
) {
}
