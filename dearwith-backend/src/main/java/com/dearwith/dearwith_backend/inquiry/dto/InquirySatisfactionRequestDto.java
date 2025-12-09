package com.dearwith.dearwith_backend.inquiry.dto;

import com.dearwith.dearwith_backend.inquiry.enums.SatisfactionStatus;
import jakarta.validation.constraints.NotNull;

public record InquirySatisfactionRequestDto(
        @NotNull SatisfactionStatus satisfactionStatus
) {}
