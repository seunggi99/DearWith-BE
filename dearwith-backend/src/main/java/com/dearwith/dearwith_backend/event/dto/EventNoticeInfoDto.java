package com.dearwith.dearwith_backend.event.dto;

import java.time.Instant;

public record EventNoticeInfoDto(
        Long id,
        String title,
        String writerNickname,
        Instant createdAt,
        Instant updatedAt
) { }
