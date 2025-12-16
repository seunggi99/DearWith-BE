package com.dearwith.dearwith_backend.event.dto;

import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.event.enums.BenefitType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventResponseDto(
        Long id,
        String title,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime openTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime closeTime,
        LocalDate startDate,
        LocalDate endDate,
        PlaceDto place,
        List<ImageGroupDto> images,
        List<ArtistDto> artists,
        List<ArtistGroupDto> artistGroups,
        List<BenefitDto> benefits,
        OrganizerDto organizer,
        Long bookmarkCount,
        boolean bookmarked,
        List<EventNoticeInfoDto> notices
) {
    public record ArtistDto(
            Long id,
            String nameKr,
            String profileImageUrl
    ) {}

    public record ArtistGroupDto(
            Long id,
            String nameKr,
            String profileImageUrl
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