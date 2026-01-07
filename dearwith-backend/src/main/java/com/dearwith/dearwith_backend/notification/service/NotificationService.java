package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.common.config.DearwithProperties;
import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.common.dto.ImageVariantDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.assembler.EventInfoAssembler;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.entity.EventNotice;
import com.dearwith.dearwith_backend.event.repository.EventNoticeRepository;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.notification.dto.NotificationResponseDto;
import com.dearwith.dearwith_backend.notification.dto.UnreadExistsResponseDto;
import com.dearwith.dearwith_backend.notification.entity.Notification;
import com.dearwith.dearwith_backend.notification.enums.NotificationType;
import com.dearwith.dearwith_backend.notification.event.PushNotificationEvent;
import com.dearwith.dearwith_backend.notification.repository.NotificationRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EventNoticeRepository eventNoticeRepository;
    private final EventRepository eventRepository;
    private final EventInfoAssembler eventInfoAssembler;
    private final UserReader userReader;
    private final PushNotificationService pushNotificationService;
    private final DearwithProperties dearwithProperties;
    private final ApplicationEventPublisher eventPublisher;

    private static final String SYSTEM_ICON_URL =
            "https://d2xzrz4ksgmdkm.cloudfront.net/inline/common/icon.png";

    /* ================================================================
       1) 안 읽은 알림 존재 여부
     ================================================================ */
    @Transactional(readOnly = true)
    public UnreadExistsResponseDto hasUnread(UUID userId) {
        if (userId == null) {
            return new UnreadExistsResponseDto(false, 0L);
        }

        userReader.getLoginAllowedUser(userId);

        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return new UnreadExistsResponseDto(count > 0, count);
    }

    /* ================================================================
       2) 알림 목록 조회
          - EVENT_NOTICE_CREATED  → targetId = noticeId
          - EVENT_CHANGED         → targetId = eventId
          - ARTIST_EVENT_CREATED  → targetId = eventId
          - SYSTEM                → targetId = systemNoticeId
     ================================================================ */
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(UUID userId,
                                                          boolean onlyUnread,
                                                          Pageable pageable) {
        userReader.getLoginAllowedUser(userId);

        Page<Notification> page = onlyUnread
                ? notificationRepository.findByUserIdAndReadFalse(userId, pageable)
                : notificationRepository.findByUserId(userId, pageable);

        List<Notification> list = page.getContent();

        // 1) EVENT_NOTICE_CREATED → noticeId 모으기
        List<Long> noticeIds = list.stream()
                .filter(n -> n.getType() == NotificationType.EVENT_NOTICE_CREATED)
                .map(Notification::getTargetId)   // targetId = noticeId
                .filter(Objects::nonNull)
                .toList();

        // 2) EVENT_CHANGED / ARTIST_EVENT_CREATED → eventId 모으기
        Set<Long> directEventIds = list.stream()
                .filter(n -> n.getType() == NotificationType.EVENT_CHANGED
                        || n.getType() == NotificationType.ARTIST_EVENT_CREATED)
                .map(Notification::getTargetId)   // targetId = eventId
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3) noticeId → eventId 매핑 (EventNotice 한 번에 조회)
        Map<Long, Long> noticeToEventIdMap = noticeIds.isEmpty()
                ? Map.of()
                : eventNoticeRepository.findByIdIn(noticeIds).stream()
                .filter(en -> en.getEvent() != null)
                .collect(Collectors.toMap(
                        EventNotice::getId,            // noticeId
                        en -> en.getEvent().getId()    // eventId
                ));

        // 4) 전체 eventId 모아서 Event 배치 조회
        Set<Long> allEventIds = new HashSet<>(directEventIds);
        allEventIds.addAll(noticeToEventIdMap.values());

        Map<Long, Event> eventMap = allEventIds.isEmpty()
                ? Map.of()
                : eventRepository.findByIdIn(allEventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e));

        return page.map(n -> toDto(n, eventMap, noticeToEventIdMap));
    }

    /* ================================================================
       3) 단일 알림 읽음 처리
     ================================================================ */
    @Transactional
    public void markAsRead(UUID userId, Long notificationId) {
        userReader.getLoginAllowedUser(userId);
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "해당 알림을 찾을 수 없습니다.",
                        "NOTIFICATION_NOT_FOUND"
                ));

        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(Instant.now());
        }
    }

    /* ================================================================
       4) 전체 읽음 처리
     ================================================================ */
    @Transactional
    public void markAllAsRead(UUID userId) {
        userReader.getLoginAllowedUser(userId);
        List<Notification> list = notificationRepository.findByUserIdAndReadFalse(userId);
        Instant now = Instant.now();

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
        userReader.getLoginAllowedUser(userId);
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
        userReader.getLoginAllowedUser(userId);
        if (onlyRead) {
            notificationRepository.deleteByUserIdAndReadTrue(userId);
        } else {
            notificationRepository.deleteByUserId(userId);
        }
    }

    /* ================================================================
    7) 알림 생성 - 이벤트 공지 등록 (여러 명)
       targetId = noticeId
    ================================================================ */
    @Transactional
    public void sendEventNoticeCreatedToMany(
            List<UUID> targetUserIds,
            Long noticeId,
            String eventTitle,
            String noticeTitle,
            boolean sendPush
    ) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return;
        }

        // 1) 인앱 알림: 대상 유저 전체에게 항상 생성
        String inAppTitle = eventTitle;
        String inAppContent = noticeTitle;

        List<Notification> notifications = targetUserIds.stream()
                .map(uid -> Notification.builder()
                        .userId(uid)
                        .type(NotificationType.EVENT_NOTICE_CREATED)
                        .title(inAppTitle)
                        .content(inAppContent)
                        .targetId(noticeId)
                        .read(false)
                        .build()
                )
                .toList();

        notificationRepository.saveAll(notifications);

        // 2) 푸시 알림: 이벤트 알림 허용한 유저만 필터링
        if (sendPush) {
            String pushTitle = "[공지] " + eventTitle;
            String pushBody = noticeTitle;
            String url = buildFullUrl(NotificationType.EVENT_NOTICE_CREATED, noticeId);

            List<UUID> pushTargets = userReader.getLoginAllowedUsers(targetUserIds).stream()
                    .filter(User::isEventNotificationEnabled)
                    .map(User::getId)
                    .toList();

            if (!pushTargets.isEmpty()) {
                eventPublisher.publishEvent(new PushNotificationEvent(
                        pushTargets, pushTitle, pushBody, url
                ));
            }
        }
    }

    /* ================================================================
       8) 알림 생성 - 이벤트 공지 등록 (1명용)
     ================================================================ */
    @Transactional
    public void sendEventNoticeCreatedToUser(
            UUID targetUserId,
            Long noticeId,
            String eventTitle,
            String noticeTitle,
            boolean sendPush
    ) {
        sendEventNoticeCreatedToMany(
                List.of(targetUserId),
                noticeId,
                eventTitle,
                noticeTitle,
                sendPush
        );
    }

    /* ================================================================
    9) 알림 생성 - 시스템 공지 (다수)
       targetId = systemNoticeId
    ================================================================ */
    @Transactional
    public void sendSystemNoticeToUsers(
            List<UUID> targetUserIds,
            Long systemNoticeId,
            String title,
            String content,
            boolean sendPush
    ) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return;
        }

        // 1) 인앱 알림: 대상 유저 전체에게 항상 생성
        List<Notification> notifications = targetUserIds.stream()
                .map(uid -> Notification.builder()
                        .userId(uid)
                        .type(NotificationType.SYSTEM)
                        .title(title)
                        .content(content)   // 상세 내용
                        .targetId(systemNoticeId)
                        .read(false)
                        .build()
                )
                .toList();

        notificationRepository.saveAll(notifications);

        // 2) 푸시 알림: 서비스 알림 허용한 유저만 필터링
        if (sendPush) {
            String pushTitle = "[디어위드] " + title;
            String pushBody = content;
            String url = buildFullUrl(NotificationType.SYSTEM, systemNoticeId);

            List<UUID> pushTargets = userReader.getLoginAllowedUsers(targetUserIds).stream()
                    .filter(User::isServiceNotificationEnabled)
                    .map(User::getId)
                    .toList();

            if (!pushTargets.isEmpty()) {
                eventPublisher.publishEvent(new PushNotificationEvent(
                        pushTargets, pushTitle, pushBody, url
                ));
            }
        }
    }

    /**
     *   전체 유저(로그인 가능한 계정)에게 시스템 공지 발송
     *    - 인앱: 전체 로그인 가능 유저
     *    - 푸시: 그 중 serviceNotificationEnabled = true 인 유저만
     */
    @Transactional
    public void sendSystemNoticeToAllUsers(
            Long systemNoticeId,
            String title,
            String content,
            boolean sendPush
    ) {
        // 전체 인앱 대상: 로그인 가능한 유저 전체
        List<User> loginAllowedUsers = userReader.getAllLoginAllowedUsers();

        List<UUID> inAppTargets = loginAllowedUsers.stream()
                .map(User::getId)
                .toList();

        // 위에 만든 공통 메서드 재사용 (여기서도 push 필터링은 안에서 처리)
        sendSystemNoticeToUsers(inAppTargets, systemNoticeId, title, content, sendPush);
    }


    /* ================================================================
       10) DTO 변환
     ================================================================ */
    private NotificationResponseDto toDto(
            Notification n,
            Map<Long, Event> eventMap,
            Map<Long, Long> noticeToEventIdMap
    ) {
        List<ImageGroupDto> cover = null;

        if (n.getType() == NotificationType.EVENT_NOTICE_CREATED
                || n.getType() == NotificationType.EVENT_CHANGED
                || n.getType() == NotificationType.ARTIST_EVENT_CREATED) {

            Long eventId = switch (n.getType()) {
                case EVENT_NOTICE_CREATED -> {
                    Long noticeId = n.getTargetId(); // targetId = noticeId
                    yield noticeId == null ? null : noticeToEventIdMap.get(noticeId);
                }
                case EVENT_CHANGED, ARTIST_EVENT_CREATED -> n.getTargetId(); // targetId = eventId
                case SYSTEM -> null;
            };

            if (eventId != null) {
                Event event = eventMap.get(eventId);
                if (event != null) {
                    cover = eventInfoAssembler.buildCoverImageGroups(event);
                }
            }
        } else if (n.getType() == NotificationType.SYSTEM) {
            cover = List.of(
                    ImageGroupDto.builder()
                            .id(null)
                            .variants(List.of(
                                    ImageVariantDto.builder()
                                            .name("logo@1x")
                                            .url(SYSTEM_ICON_URL)
                                            .build()
                            ))
                            .build()
            );
        }

        String linkUrl = buildFullUrl(n.getType(), n.getTargetId());

        return new NotificationResponseDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getContent(),
                linkUrl,
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt(),
                cover
        );
    }

    /* ================================================================
       11) 링크 URL 조립
       - EVENT_NOTICE_CREATED → /notices/{noticeId}
       - EVENT_CHANGED        → /events/{eventId}
       - ARTIST_EVENT_CREATED → /events/{eventId}
       - SYSTEM               → /system-notices/{id}
     ================================================================ */
    private String buildFullUrl(NotificationType type, Long targetId) {
        String base = dearwithProperties.getBaseUrl();

        if (targetId == null) {
            throw BusinessException.withMessageAndDetail(ErrorCode.NOT_FOUND,
                    "알 수 없는 오류가 발생했습니다.",
                    "NOTIFICATION_TARGET_ID_NULL");
        }

        return switch (type) {
            case EVENT_NOTICE_CREATED ->
                    base + "/notice-detail/" + targetId;
            case EVENT_CHANGED, ARTIST_EVENT_CREATED ->
                    base + "/event-detail/" + targetId;
            case SYSTEM ->
                    base + "/system-notice-detail/" + targetId;
        };
    }
}