package com.dearwith.dearwith_backend.notice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoticeUpdateRequestDto {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private boolean important;
}
