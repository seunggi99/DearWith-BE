package com.dearwith.dearwith_backend.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventNoticeRequestDto(
        @NotBlank
        @Size(max = 50)
        String title,

        @NotBlank
        @Size(max = 300)
        String content
) {
}
