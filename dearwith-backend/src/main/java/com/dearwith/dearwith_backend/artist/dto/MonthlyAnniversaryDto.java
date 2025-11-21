package com.dearwith.dearwith_backend.artist.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

public record MonthlyAnniversaryDto(
        Long id,
        String nameKr,
        String nameEn,
        String imageUrl,
        Type type,
        LocalDate date,
        boolean isToday,
        Integer years
) {
    public enum Type {
        ARTIST,
        GROUP
    }
}