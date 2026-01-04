package com.dearwith.dearwith_backend.artist.dto;

import com.dearwith.dearwith_backend.artist.enums.ArtistType;

import java.time.LocalDate;

public record MonthlyAnniversaryDto(
        Long id,
        String nameKr,
        String imageUrl,
        ArtistType type,
        DateType dateType,
        LocalDate date,
        boolean isToday,
        Integer years
) {
    public enum DateType {
        DEBUT,
        BIRTH
    }
}