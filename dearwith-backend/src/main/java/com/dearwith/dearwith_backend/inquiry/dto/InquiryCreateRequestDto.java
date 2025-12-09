package com.dearwith.dearwith_backend.inquiry.dto;

import jakarta.validation.constraints.NotBlank;

public record InquiryCreateRequestDto(
        @NotBlank
        String title,

        @NotBlank
        String content
) {}
