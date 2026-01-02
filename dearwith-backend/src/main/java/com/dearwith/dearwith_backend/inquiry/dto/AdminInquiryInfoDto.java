package com.dearwith.dearwith_backend.inquiry.dto;

public record AdminInquiryInfoDto(
        Long id,
        String title,
        boolean answered,
        String userNickname,
        java.time.Instant createdAt
) {}
