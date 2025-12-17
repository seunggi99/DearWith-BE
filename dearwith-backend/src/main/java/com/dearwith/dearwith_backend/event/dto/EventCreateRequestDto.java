package com.dearwith.dearwith_backend.event.dto;

import com.dearwith.dearwith_backend.event.enums.BenefitType;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventCreateRequestDto(
        @NotBlank(message = "이벤트 명을 입력해주세요.")
        @Pattern(
                regexp = "^[^\\p{Cntrl}]+$",
                message = "국문, 영문, 숫자, 특수문자만 입력해주세요."
        )
        @Size(max = 50, message = "이벤트 제목은 최대 50자까지 입력할 수 있습니다.")
        String title,

        @NotNull(message = "이벤트 운영 시간을 입력해주세요.")
        @Schema(example = "10:00")
        LocalTime openTime,
        @NotNull(message = "이벤트 운영 시간을 입력해주세요.")
        @Schema(example = "18:00")
        LocalTime closeTime,
        @NotNull(message = "이벤트 기간을 입력해주세요.")
        LocalDate startDate,
        @NotNull(message = "이벤트 기간을 입력해주세요.")
        LocalDate endDate,
        List<Long> artistIds,
        List<Long> artistGroupIds,
        @NotNull(message = "이벤트 장소를 입력해주세요.")
        PlaceDto place,
        @Size(min = 1, max = 10, message = "이벤트 이미지는 최대 10개까지 등록할 수 있습니다.")
        @NotNull(message = "이벤트 이미지는 필수입니다.")
        List<@Valid ImageAttachmentRequestDto> images,
        List<BenefitDto> benefits,
        @NotNull(message = "주최자 정보는 필수입니다.")
        OrganizerDto organizer,
        @Size(max = 32, message = "X 핸들은 최대 32자까지 입력할 수 있습니다.")
        @Schema(example = "https://x.com/dearwith_kr")
        @Pattern(
                regexp = "^(https?://)?([\\w-]+\\.)+[\\w-]+(/.*)?$",
                message = "올바른 URL 형식이 아닙니다."
        )
        String xLink
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
            @NotBlank(message = "특전 이름을 입력해주세요.")
            @Pattern(
                    regexp = "^[^\\p{Cntrl}]+$",
                    message = "특전 이름에 사용할 수 없는 문자가 포함되어 있습니다."
            )
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
            @NotBlank(message = "X 계정 링크를 입력해주세요.")
            @Size(max = 32, message = "X 핸들은 최대 32자까지 입력할 수 있습니다.")
            @Schema(example = "dearwith_kr")
            @Pattern(
                    regexp = "^[A-Za-z0-9._-]+$",
                    message = "X 계정 링크를 다시 입력해주세요."
            )
            String xHandle,
            String xTicket
    ) {}
}