package com.dearwith.dearwith_backend.inquiry.dto;

import com.dearwith.dearwith_backend.inquiry.enums.SatisfactionStatus;

import java.time.LocalDateTime;

public record InquiryResponseDto(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        boolean answered,
        SatisfactionStatus satisfactionStatus,
        AnswerDto answer
) {
    public record AnswerDto(
            String content,
            LocalDateTime answeredAt
    ) {}
}