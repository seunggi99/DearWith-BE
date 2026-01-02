package com.dearwith.dearwith_backend.notification.dto;

import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.notification.enums.NotificationType;

import java.time.Instant;
import java.util.List;

public record NotificationResponseDto(
        Long id,
        NotificationType type,
        String title,
        String content,
        String linkUrl,
        boolean read,
        Instant readAt,
        Instant createdAt,
        List<ImageGroupDto> coverImage
) {}