package com.dearwith.dearwith_backend.notification.dto;

import com.dearwith.dearwith_backend.notification.enums.NotificationType;
import lombok.Getter;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long id,
        NotificationType type,
        String title,
        String content,
        String linkUrl,
        Long targetId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {}