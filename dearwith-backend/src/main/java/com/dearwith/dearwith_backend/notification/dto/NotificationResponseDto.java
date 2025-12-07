package com.dearwith.dearwith_backend.notification.dto;

import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.notification.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationResponseDto(
        Long id,
        NotificationType type,
        String title,
        String content,
        Long eventId,
        Long noticeId,
        Long systemNoticeId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        List<ImageGroupDto> coverImage
) {}