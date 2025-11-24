package com.dearwith.dearwith_backend.event.dto;

import com.dearwith.dearwith_backend.event.enums.BenefitType;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentUpdateRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventUpdateRequestDto(
        @Size(max = 50, message = "이벤트 제목은 최대 50자까지 입력할 수 있습니다.")
        String title,
        LocalTime openTime,
        LocalTime closeTime,
        LocalDate startDate,
        LocalDate endDate,
        List<Long> artistIds,
        List<Long> artistGroupIds,
        EventCreateRequestDto.PlaceDto place,
        @Size(max = 10, message = "이벤트 이미지는 최대 10개까지 등록할 수 있습니다.")
        List<@Valid ImageAttachmentUpdateRequestDto> images,
        List<EventCreateRequestDto.BenefitDto> benefits
) { }
