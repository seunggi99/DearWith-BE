package com.dearwith.dearwith_backend.event.dto;

import com.dearwith.dearwith_backend.event.enums.BenefitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EventCreateRequestDto(
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        List<Long> artistIds,
        PlaceDto place,
        List<ImageAttachDto> images,
        List<BenefitDto> benefits,
        OrganizerDto organizer

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

    public record ImageAttachDto(
            String tmpKey,
            Integer displayOrder
    ) {}

    public record BenefitDto(
            String name,
            String description,
            BenefitType benefitType,
            Integer dayIndex,
            Integer displayOrder
    ) {}

    public record OrganizerDto(
            boolean verified,
            String xHandle,
            String xTicket
    ) {}
}