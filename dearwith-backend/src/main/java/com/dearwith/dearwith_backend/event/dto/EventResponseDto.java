package com.dearwith.dearwith_backend.event.dto;

import com.dearwith.dearwith_backend.artist.dto.ArtistGroupDto;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.event.enums.BenefitType;
import org.w3c.dom.ls.LSException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventResponseDto(
        Long id,
        String title,
        LocalTime openTime,
        LocalTime closeTime,
        LocalDate startDate,
        LocalDate endDate,
        PlaceDto place,
        List<ImageDto> images,
        List<ArtistDto> artists,
        List<ArtistGroupDto> artistGroups,
        List<BenefitDto> benefits,
        OrganizerDto organizer,
        Long bookmarkCount,
        boolean bookmarked
) {
    public record ArtistDto(
            Long id,
            String nameKr,
            String nameEn,
            String profileImageUrl
    ) {}

    public record ArtistGroupDto(
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