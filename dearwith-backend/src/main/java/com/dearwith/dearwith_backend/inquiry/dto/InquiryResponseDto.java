package com.dearwith.dearwith_backend.inquiry.dto;

import com.dearwith.dearwith_backend.inquiry.enums.SatisfactionStatus;

import java.time.Instant;

public record InquiryResponseDto(
        Long id,
        String title,
        String content,
        Instant createdAt,
        boolean answered,
        SatisfactionStatus satisfactionStatus,
        AnswerDto answer
) {
    public record AnswerDto(
            String content,
            Instant answeredAt
    ) {}
}