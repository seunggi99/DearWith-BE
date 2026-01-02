package com.dearwith.dearwith_backend.inquiry.dto;

import com.dearwith.dearwith_backend.inquiry.enums.SatisfactionStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminInquiryResponseDto (
        Long id,
        String title,
        String content,
        Instant createdAt,
        boolean answered,
        SatisfactionStatus satisfactionStatus,
        AnswerDto answer,
        UserInfoDto user
) {
    public record AnswerDto(
            String content,
            Instant answeredAt,
            String adminNickname
    ) {}

    public record UserInfoDto(
            UUID id,
            String nickname
    ) {}
}
