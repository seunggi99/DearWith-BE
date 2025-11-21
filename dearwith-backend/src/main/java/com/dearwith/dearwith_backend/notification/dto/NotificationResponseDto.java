package com.dearwith.dearwith_backend.notification.dto;

import com.dearwith.dearwith_backend.notification.enums.NotificationType;
import lombok.Getter;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long id,
        NotificationType type,
        String title,
        String content,
        Long eventId,
        Long noticeId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {}