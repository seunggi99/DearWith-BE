package com.dearwith.dearwith_backend.artist.dto;

import java.time.LocalDate;

public record MonthlyAnniversaryDto(
        Long id,
        String nameKr,
        String imageUrl,
        Type type,
        DateType dateType,
        LocalDate date,
        boolean isToday,
        Integer years
) {
    public enum Type {
        ARTIST,
        GROUP
    }
    public enum DateType {
        DEBUT,
        BIRTH
    }
}