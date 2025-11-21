package com.dearwith.dearwith_backend.event.dto;

import com.dearwith.dearwith_backend.event.enums.BenefitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventUpdateRequestDto(
        String title,
        LocalTime openTime,
        LocalTime closeTime,
        LocalDate startDate,
        LocalDate endDate,
        List<Long> artistIds,
        List<Long> artistGroupIds,
        PlaceDto place,
        List<ImageUpdateDto> images,
        List<BenefitDto> benefits
) {

    public record PlaceDto(
            String kakaoPlaceId,
            String name,
            String roadAddress,
            String jibunAddress,
            BigDecimal lon,
            BigDecimal lat,
            String phone,
            String placeUrl
    ) {}

    public record ImageUpdateDto(
            Long id,
            String tmpKey,
            Integer displayOrder
    ) {}

    public record BenefitDto(
            String name,
            BenefitType benefitType,
            Integer dayIndex,
            Integer displayOrder
    ) {}
}
