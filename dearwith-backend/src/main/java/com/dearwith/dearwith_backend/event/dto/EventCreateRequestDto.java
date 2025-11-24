package com.dearwith.dearwith_backend.event.dto;

import com.dearwith.dearwith_backend.event.enums.BenefitType;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventCreateRequestDto(
        @NotBlank(message = "이벤트 제목은 필수입니다.")
        @Size(max = 50, message = "이벤트 제목은 최대 50자까지 입력할 수 있습니다.")
        String title,
        LocalTime openTime,
        LocalTime closeTime,
        LocalDate startDate,
        LocalDate endDate,
        List<Long> artistIds,
        List<Long> artistGroupIds,
        PlaceDto place,
        @Size(max = 10, message = "이벤트 이미지는 최대 10개까지 등록할 수 있습니다.")
        List<@Valid ImageAttachmentRequestDto> images,
        List<BenefitDto> benefits,
        @NotNull(message = "주최자 정보는 필수입니다.")
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
    public record BenefitDto(
            @NotBlank(message = "특전 이름은 필수입니다.")
            @Size(max = 30, message = "특전 이름은 최대 30자까지 입력할 수 있습니다.")
            String name,
            @NotNull(message = "특전 유형은 필수입니다.")
            BenefitType benefitType,
            Integer dayIndex,
            @NotNull
            Integer displayOrder
    ) {}

    public record OrganizerDto(
            boolean verified,
            String xHandle,
            String xTicket
    ) {}
}