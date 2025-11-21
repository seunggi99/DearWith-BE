package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.notification.dto.NotificationResponseDto;
import com.dearwith.dearwith_backend.notification.dto.UnreadExistsResponseDto;
import com.dearwith.dearwith_backend.notification.entity.Notification;
import com.dearwith.dearwith_backend.notification.enums.NotificationType;
import com.dearwith.dearwith_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    // ================================================================
    // 1) 안 읽은 알림 존재 여부 + 개수
    // ================================================================
    public UnreadExistsResponseDto hasUnread(UUID userId) {
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return new UnreadExistsResponseDto(count > 0, count);
    }

    // ================================================================
    // 2) 알림 목록 조회 (전체 / onlyUnread)
    // ================================================================
    public Page<NotificationResponseDto> getNotifications(UUID userId, boolean onlyUnread, Pageable pageable) {

        Page<Notification> page = onlyUnread
                ? notificationRepository.findByUserIdAndReadFalse(userId, pageable)
                : notificationRepository.findByUserId(userId, pageable);

        return page.map(this::toDto);
    }

    // ================================================================
    // 3) 단일 알림 읽음 처리
    // ================================================================
    @Transactional
    public void markAsRead(UUID userId, Long notificationId) {

        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
    }

    // ================================================================
    // 4) 전체 알림 읽음 처리
    // ================================================================
    @Transactional
    public void markAllAsRead(UUID userId) {

        List<Notification> list = notificationRepository
                .findByUserIdAndReadFalse(userId);

        LocalDateTime now = LocalDateTime.now();

        for (Notification n : list) {
            n.setRead(true);
            n.setReadAt(now);
        }
    }

    // ================================================================
    // 5) 단일 알림 삭제
    // ================================================================
    @Transactional
    public void deleteOne(UUID userId, Long notificationId) {

        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        notificationRepository.delete(notification);
    }

    // ================================================================
    // 6) 전체 알림 삭제 (옵션: 읽은 것만 삭제)
    // ================================================================
    @Transactional
    public void deleteAll(UUID userId, boolean onlyRead) {

        if (onlyRead) {
            notificationRepository.deleteByUserIdAndReadTrue(userId);
        } else {
            notificationRepository.deleteByUserId(userId);
        }
    }

    // =========================================================
    // [알림 생성] 이벤트 공지 등록 알림
    // =========================================================
    @Transactional
    public void sendEventNoticeCreated(
            UUID targetUserId,   // 알림 받을 유저
            Long eventId,
            Long noticeId,
            String eventTitle,
            String noticeTitle
    ) {
        String title   = eventTitle;      // 알림 상단: 이벤트 제목
        String content = noticeTitle;     // 알림 하단: 공지 제목
        String linkUrl = "/events/" + eventId + "#notice-" + noticeId;

        Notification n = new Notification();
        n.setUserId(targetUserId);
        n.setType(NotificationType.EVENT_NOTICE_CREATED);
        n.setTitle(title);
        n.setContent(content);
        n.setLinkUrl(linkUrl);
        n.setTargetId(noticeId);
        n.setRead(false);
        n.setReadAt(null);

        notificationRepository.save(n);
    }

    @Transactional
    public void sendEventNoticeCreatedToMany(
            List<UUID> targetUserIds,
            Long eventId,
            Long noticeId,
            String eventTitle,
            String noticeTitle
    ) {
        if (targetUserIds == null || targetUserIds.isEmpty()) return;

        for (UUID uid : targetUserIds) {
            sendEventNoticeCreated(
                    uid,
                    eventId,
                    noticeId,
                    eventTitle,
                    noticeTitle
            );
        }
    }

    public NotificationResponseDto toDto(Notification n) {
        Long eventId = null;
        Long noticeId = null;

        switch (n.getType()) {
            case EVENT_NOTICE_CREATED -> {
                if (n.getLinkUrl() != null) {
                    String url = n.getLinkUrl();
                    try {
                        Pattern p = Pattern.compile("/events/(\\d+)(?:#notice-(\\d+))?");
                        Matcher m = p.matcher(url);
                        if (m.find()) {
                            eventId = Long.valueOf(m.group(1));
                            if (m.group(2) != null) {
                                noticeId = Long.valueOf(m.group(2));
                            }
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
            case SYSTEM -> {
            }
        }

        return new NotificationResponseDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getContent(),
                eventId,
                noticeId,
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
