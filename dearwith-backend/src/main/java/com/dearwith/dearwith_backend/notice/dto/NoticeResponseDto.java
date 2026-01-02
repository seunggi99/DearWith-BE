package com.dearwith.dearwith_backend.notice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NoticeResponseDto {

    private Long id;
    private String title;
    private String content;
    private boolean important;
    private boolean pushEnabled;
    private Instant createdAt;
}