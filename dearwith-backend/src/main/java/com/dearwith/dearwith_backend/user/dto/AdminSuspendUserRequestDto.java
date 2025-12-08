package com.dearwith.dearwith_backend.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AdminSuspendUserRequestDto(
        @NotBlank(message = "정지 사유는 필수입니다.")
        String reason,
        LocalDate until
) {}
