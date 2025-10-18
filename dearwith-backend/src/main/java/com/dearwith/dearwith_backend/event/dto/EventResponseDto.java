package com.dearwith.dearwith_backend.event.dto;

import com.dearwith.dearwith_backend.event.enums.BenefitType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EventResponseDto(
        Long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        PlaceDto place,
        List<ImageDto> images,
        List<ArtistDto> artists,
        List<BenefitDto> benefits,
        OrganizerDto organizer
) {
    public record ArtistDto(
            Long id,
            String nameKr,
            String nameEn,
            String profileImageUrl
    ) {}

    public record ImageDto(
            Long id,
            String imageUrl,
            Integer displayOrder
    ) {}

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

    public record BenefitDto(
            Long id,
            String name,
            String description,
            BenefitType benefitType,
            Integer dayIndex,
            LocalDate visibleFrom,
            Integer displayOrder
    ) {}

    public record OrganizerDto(
            boolean verified,
            String xHandle,
            String xId,
            String xName
    ) {}
}