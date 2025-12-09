package com.dearwith.dearwith_backend.inquiry.dto;

import com.dearwith.dearwith_backend.inquiry.enums.SatisfactionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminInquiryResponseDto (
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        boolean answered,
        SatisfactionStatus satisfactionStatus,
        AnswerDto answer,
        UserInfoDto user
) {
    public record AnswerDto(
            String content,
            LocalDateTime answeredAt,
            String adminNickname
    ) {}

    public record UserInfoDto(
            UUID id,
            String nickname
    ) {}
}
