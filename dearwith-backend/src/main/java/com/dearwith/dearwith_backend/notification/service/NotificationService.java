package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.assembler.EventInfoAssembler;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final EventInfoAssembler eventInfoAssembler;
    private final EventRepository eventRepository;
    private static final Pattern EVENT_NOTICE_PATTERN =
            Pattern.compile("/events/(\\d+)(?:#notice-(\\d+))?");

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

        // 1) 알림 페이지 조회
        Page<Notification> page = onlyUnread
                ? notificationRepository.findByUserIdAndReadFalse(userId, pageable)
                : notificationRepository.findByUserId(userId, pageable);

        List<Notification> notifications = page.getContent();

        // 2) 알림들에서 eventId만 먼저 뽑아서 배치 조회할 준비
        Set<Long> eventIds = notifications.stream()
                .map(this::parseEventLink)   // EventLink
                .map(EventLink::eventId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3) 이벤트 배치 조회
        Map<Long, Event> eventMap = eventIds.isEmpty()
                ? Map.of()
                : eventRepository.findByIdIn(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e));

        // 4) 매핑
        return page.map(n -> toDto(n, eventMap));
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

    private NotificationResponseDto toDto(Notification n, Map<Long, Event> eventMap) {

        Long eventId = null;
        Long noticeId = null;
        List<ImageGroupDto> coverImage = null;

        if (n.getType() == NotificationType.EVENT_NOTICE_CREATED) {
            EventLink link = parseEventLink(n);
            eventId = link.eventId();
            noticeId = link.noticeId();

            if (eventId != null) {
                Event event = eventMap.get(eventId);
                if (event != null) {
                    coverImage = eventInfoAssembler.buildCoverImageGroups(event);
                }
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
                n.getCreatedAt(),
                coverImage
        );
    }

    private EventLink parseEventLink(Notification n) {
        if (n.getType() != NotificationType.EVENT_NOTICE_CREATED) {
            return new EventLink(null, null);
        }

        String url = n.getLinkUrl();
        if (url == null || url.isBlank()) {
            return new EventLink(null, null);
        }

        try {
            Matcher m = EVENT_NOTICE_PATTERN.matcher(url);
            if (m.find()) {
                Long eventId = Long.valueOf(m.group(1));
                Long noticeId = null;

                if (m.group(2) != null) {
                    noticeId = Long.valueOf(m.group(2));
                }

                return new EventLink(eventId, noticeId);
            }
        } catch (Exception ignore) {
        }

        return new EventLink(null, null);
    }


    private record EventLink(Long eventId, Long noticeId) {}
}
