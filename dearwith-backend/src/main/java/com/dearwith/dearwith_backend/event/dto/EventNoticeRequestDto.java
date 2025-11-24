package com.dearwith.dearwith_backend.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventNoticeRequestDto(
        @NotBlank
        @Size(max = 50, message = "공지 제목은 최대 50자까지 입력할 수 있습니다.")
        String title,

        @NotBlank
        @Size(max = 300, message = "공지 내용은 최대 300자까지 입력할 수 있습니다.")
        String content
) {
}
