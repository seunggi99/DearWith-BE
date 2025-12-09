package com.dearwith.dearwith_backend.inquiry.dto;

import java.time.LocalDateTime;

public record InquiryInfoDto(
        Long id,
        String title,
        LocalDateTime createdAt,
        boolean answered
) {}