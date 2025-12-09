package com.dearwith.dearwith_backend.inquiry.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminInquiryAnswerRequestDto(
        @NotBlank String content
) {}
