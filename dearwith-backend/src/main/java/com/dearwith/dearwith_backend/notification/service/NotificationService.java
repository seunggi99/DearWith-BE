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

    /* ================================================================
       1) 안 읽은 알림 존재 여부
     ================================================================ */
    public UnreadExistsResponseDto hasUnread(UUID userId) {
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return new UnreadExistsResponseDto(count > 0, count);
    }

    /* ================================================================
       2) 알림 목록 조회
     ================================================================ */
    public Page<NotificationResponseDto> getNotifications(UUID userId, boolean onlyUnread, Pageable pageable) {

        Page<Notification> page = onlyUnread
                ? notificationRepository.findByUserIdAndReadFalse(userId, pageable)
                : notificationRepository.findByUserId(userId, pageable);

        List<Notification> list = page.getContent();

        // eventId 추출
        Set<Long> eventIds = list.stream()
                .map(this::parseEventLink)
                .map(EventLink::eventId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 이벤트 배치 조회
        Map<Long, Event> eventMap = eventIds.isEmpty()
                ? Map.of()
                : eventRepository.findByIdIn(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e));

        return page.map(n -> toDto(n, eventMap));
    }

    /* ================================================================
       3) 단일 알림 읽음 처리
     ================================================================ */
    @Transactional
    public void markAsRead(UUID userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "해당 알림을 찾을 수 없습니다.",
                        "NOTIFICATION_NOT_FOUND"
                ));

        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
        }
    }

    /* ================================================================
       4) 전체 읽음 처리
     ================================================================ */
    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> list = notificationRepository.findByUserIdAndReadFalse(userId);
        LocalDateTime now = LocalDateTime.now();

        for (Notification n : list) {
            n.setRead(true);
            n.setReadAt(now);
        }
    }

    /* ================================================================
       5) 단일 삭제
     ================================================================ */
    @Transactional
    public void deleteOne(UUID userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "해당 알림을 찾을 수 없습니다.",
                        "NOTIFICATION_NOT_FOUND"
                ));

        notificationRepository.delete(n);
    }

    /* ================================================================
       6) 전체 삭제
     ================================================================ */
    @Transactional
    public void deleteAll(UUID userId, boolean onlyRead) {
        if (onlyRead) {
            notificationRepository.deleteByUserIdAndReadTrue(userId);
        } else {
            notificationRepository.deleteByUserId(userId);
        }
    }

    /* ================================================================
       7) 알림 생성 (개인)
     ================================================================ */
    @Transactional
    public void sendEventNoticeCreated(
            UUID targetUserId,
            Long eventId,
            Long noticeId,
            String eventTitle,
            String noticeTitle
    ) {
        Notification n = new Notification();
        n.setUserId(targetUserId);
        n.setType(NotificationType.EVENT_NOTICE_CREATED);
        n.setTitle(eventTitle);
        n.setContent(noticeTitle);
        n.setLinkUrl("/events/" + eventId + "#notice-" + noticeId);
        n.setTargetId(noticeId);
        n.setRead(false);

        notificationRepository.save(n);
    }

    /* ================================================================
       8) 알림 생성 (여러명)
     ================================================================ */
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
            sendEventNoticeCreated(uid, eventId, noticeId, eventTitle, noticeTitle);
        }
    }

    /* ================================================================
       9) DTO 변환
     ================================================================ */
    private NotificationResponseDto toDto(Notification n, Map<Long, Event> eventMap) {

        Long eventId = null;
        Long noticeId = null;
        List<ImageGroupDto> cover = null;

        if (n.getType() == NotificationType.EVENT_NOTICE_CREATED) {
            EventLink link = parseEventLink(n);
            eventId = link.eventId();
            noticeId = link.noticeId();

            if (eventId != null) {
                Event event = eventMap.get(eventId);
                if (event != null) {
                    cover = eventInfoAssembler.buildCoverImageGroups(event);
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
                cover
        );
    }

    /* ================================================================
       10) 링크 파싱
       - 규약이 깨져도 예외 던지면 안 됨 (프론트/백에서 URL 오염 가능)
       - null 반환하고 정상 동작해야 UX 유지됨
     ================================================================ */
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
                Long noticeId = (m.group(2) != null) ? Long.valueOf(m.group(2)) : null;
                return new EventLink(eventId, noticeId);
            }
        } catch (Exception ignore) { }

        return new EventLink(null, null);
    }

    private record EventLink(Long eventId, Long noticeId) {}
}