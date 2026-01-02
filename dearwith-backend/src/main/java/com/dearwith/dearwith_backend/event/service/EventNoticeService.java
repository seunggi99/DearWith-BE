package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.common.component.ViewCountLimiter;
import com.dearwith.dearwith_backend.common.dto.CreatedResponseDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.dto.EventNoticeInfoDto;
import com.dearwith.dearwith_backend.event.dto.EventNoticeListResponseDto;
import com.dearwith.dearwith_backend.event.dto.EventNoticeRequestDto;
import com.dearwith.dearwith_backend.event.dto.EventNoticeResponseDto;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.entity.EventNotice;
import com.dearwith.dearwith_backend.event.repository.EventBookmarkRepository;
import com.dearwith.dearwith_backend.event.repository.EventNoticeRepository;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.notification.service.NotificationService;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.service.UserReader;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventNoticeService {

    private final EventNoticeRepository eventNoticeRepository;
    private final EventRepository eventRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final ViewCountLimiter viewCountLimiter;
    private final UserReader userReader;

    /*────────────────────────────
     | 개별 공지 조회
     *────────────────────────────*/
    @Transactional
    public EventNoticeResponseDto getNoticeById(Long noticeId, UUID userId) {
        EventNotice notice = eventNoticeRepository.findById(noticeId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "해당 공지를 찾을 수 없습니다."
                ));
        if (userId == null){
            userReader.getLoginAllowedUser(userId);
        }

        if (userId != null &&
                viewCountLimiter.shouldIncrease("EVENT_NOTICE", noticeId, userId)) {

            eventNoticeRepository.incrementViewCount(noticeId);
        }

        boolean editable = userId != null
                && Objects.equals(userId, notice.getUser().getId());

        return toDetailDto(notice, editable);
    }

    /*────────────────────────────
     | 공지 생성
     *────────────────────────────*/
    @Transactional
    public CreatedResponseDto create(UUID userId, Long eventId, EventNoticeRequestDto req) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "이벤트를 찾을 수 없습니다."
                ));

        User user = userReader.getActiveUser(userId);

        authService.validateOwner(event.getUser(), user, "이벤트 공지를 작성할 권한이 없습니다.");

        EventNotice notice = EventNotice.builder()
                .event(event)
                .user(user)
                .title(req.title())
                .content(req.content())
                .build();

        EventNotice saved = eventNoticeRepository.save(notice);

        // 북마크한 사람들에게 알림 전송
        try {
            List<UUID> targetUserIds =
                    eventBookmarkRepository.findUserIdsByEventId(event.getId());

            notificationService.sendEventNoticeCreatedToMany(
                    targetUserIds,
                    notice.getId(),
                    event.getTitle(),
                    notice.getTitle(),
                    true
            );

        } catch (Exception e) {
            throw BusinessException.withAll(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "공지 등록은 완료되었지만 알림 전송에 실패했습니다.",
                    "NOTICE_NOTIFICATION_FAILED",
                    "Failed to send notification: " + e.getMessage(),
                    e
            );
        }

        return CreatedResponseDto.builder()
                .id(saved.getId())
                .build();
    }

    /*────────────────────────────
     | 공지 수정
     *────────────────────────────*/
    @Transactional
    public void update(UUID userId, Long eventId, Long noticeId, EventNoticeRequestDto req) {

        User user = userReader.getActiveUser(userId);

        EventNotice notice = eventNoticeRepository.findById(noticeId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "이벤트 공지를 찾을 수 없습니다."
                ));

        if (!notice.getEvent().getId().equals(eventId)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    null,
                    "NOTICE_EVENT_MISMATCH"
            );
        }

        authService.validateOwner(notice.getUser(), user, "이벤트 공지를 수정할 권한이 없습니다.");

        notice.update(req.title(), req.content());
    }

    /*────────────────────────────
     | 공지 삭제 (soft delete)
     *────────────────────────────*/
    @Transactional
    public void delete(UUID userId, Long eventId, Long noticeId) {
        User user = userReader.getLoginAllowedUser(userId);

        EventNotice notice = eventNoticeRepository.findById(noticeId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "이벤트 공지를 찾을 수 없습니다."
                ));

        if (!notice.getEvent().getId().equals(eventId)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    null,
                    "NOTICE_EVENT_MISMATCH"
            );
        }

        authService.validateOwner(notice.getUser(), user, "이벤트 공지를 삭제할 권한이 없습니다.");

        notice.softDelete();
    }

    /*────────────────────────────
     | 이벤트별 공지 목록 조회
     *────────────────────────────*/
    @Transactional
    public EventNoticeListResponseDto getNoticesByEvent(Long eventId, UUID userId, Pageable pageable) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "이벤트를 찾을 수 없습니다."
                ));

        boolean writable = false;

        if (userId != null) {
            User user = userReader.getActiveUser(userId);
            writable = (event.getUser() != null
                    && event.getUser().getId() != null
                    && event.getUser().getId().equals(user.getId()));
        }


        Page<EventNotice> page = eventNoticeRepository.findByEventId(eventId, pageable);

        Page<EventNoticeInfoDto> noticeDtos = page.map(this::toInfoDto);

        return new EventNoticeListResponseDto(
                eventId,
                writable,
                noticeDtos
        );
    }

    /*────────────────────────────
     | 최신 공지 5개 조회
     *────────────────────────────*/
    @Transactional
    public List<EventNoticeInfoDto> getLatestNoticesForEvent(Long eventId) {
        Pageable pageable = PageRequest.of(
                0,
                5,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EventNotice> page = eventNoticeRepository.findByEventId(eventId, pageable);

        return page.getContent().stream()
                .map(this::toInfoDto)
                .toList();
    }

    /*────────────────────────────
     | Mapper
     *────────────────────────────*/

    private EventNoticeInfoDto toInfoDto(EventNotice notice) {
        return new EventNoticeInfoDto(
                notice.getId(),
                notice.getTitle(),
                notice.getUser() != null ? notice.getUser().getNickname() : null,
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    private EventNoticeResponseDto toDetailDto(EventNotice notice, boolean editable) {
        return new EventNoticeResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getUser() != null ? notice.getUser().getNickname() : null,
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                editable
        );
    }
}