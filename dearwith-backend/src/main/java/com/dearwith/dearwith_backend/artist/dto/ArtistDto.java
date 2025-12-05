package com.dearwith.dearwith_backend.artist.dto;

import java.time.LocalDate;

public record ArtistDto(
        Long id,
        String nameKr,
        String imageUrl,
        LocalDate birthDate,
        LocalDate debutDate
) {}
