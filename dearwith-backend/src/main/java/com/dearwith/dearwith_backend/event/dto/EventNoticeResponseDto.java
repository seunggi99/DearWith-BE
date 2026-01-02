package com.dearwith.dearwith_backend.event.dto;

import java.time.Instant;

public record EventNoticeResponseDto (
        Long id,
        String title,
        String content,
        String writerNickname,
        Instant createdAt,
        Instant updatedAt,
        boolean editable
) {
}
