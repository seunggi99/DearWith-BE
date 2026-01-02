package com.dearwith.dearwith_backend.inquiry.dto;

import java.time.Instant;

public record InquiryInfoDto(
        Long id,
        String title,
        Instant createdAt,
        boolean answered
) {}