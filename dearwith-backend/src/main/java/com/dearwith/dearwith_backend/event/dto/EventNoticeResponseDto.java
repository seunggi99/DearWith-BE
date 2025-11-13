package com.dearwith.dearwith_backend.event.dto;

import java.time.LocalDateTime;

public record EventNoticeResponseDto(
        Long id,
        Long eventId,
        String title,
        String content,
        String writerNickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}