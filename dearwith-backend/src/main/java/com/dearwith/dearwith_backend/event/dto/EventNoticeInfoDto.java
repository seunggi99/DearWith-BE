package com.dearwith.dearwith_backend.event.dto;

import java.time.LocalDateTime;

public record EventNoticeInfoDto(
        Long id,
        String title,
        String writerNickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) { }
