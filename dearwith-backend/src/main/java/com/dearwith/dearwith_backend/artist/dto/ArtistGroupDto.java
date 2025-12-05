package com.dearwith.dearwith_backend.artist.dto;

import java.time.LocalDate;

public record ArtistGroupDto (
        Long id,
        String nameKr,
        String description,
        LocalDate debutDate,
        String imageUrl
) {
}
