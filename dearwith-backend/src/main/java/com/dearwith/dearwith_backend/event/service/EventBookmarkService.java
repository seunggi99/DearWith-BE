package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.assembler.EventInfoAssembler;
import com.dearwith.dearwith_backend.event.dto.EventBookmarkResponseDto;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.entity.EventBookmark;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import com.dearwith.dearwith_backend.event.repository.EventBookmarkRepository;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EventBookmarkService {

    private final EventRepository eventRepository;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final HotEventService hotEventService;
    private final EventInfoAssembler eventInfoAssembler;
    private final UserReader userReader;

    /*──────────────────────────────────────────────
     | 1. 북마크 추가
     *──────────────────────────────────────────────*/
    @Transactional
    public EventBookmarkResponseDto addBookmark(Long eventId, UUID userId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "이벤트를 찾을 수 없습니다."
                ));

        User user = userReader.getLoginAllowedUser(userId);

        try {
            eventBookmarkRepository.save(EventBookmark.builder()
                    .event(event)
                    .user(user)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ALREADY_BOOKMARKED,
                    "이미 찜한 이벤트입니다.",
                    "EVENT_ALREADY_BOOKMARKED"
            );
        }

        // 북마크 수 증가
        eventRepository.incrementBookmark(eventId);

        // 핫 이벤트 점수 반영
        hotEventService.increaseEventScore(eventId, HotEventService.Action.BOOKMARK);

        long count = eventRepository.getBookmarkCount(eventId);

        return new EventBookmarkResponseDto(
                eventId,
                true,
                count
        );
    }

    /*──────────────────────────────────────────────
     | 2. 북마크 제거
     *──────────────────────────────────────────────*/
    @Transactional
    public EventBookmarkResponseDto removeBookmark(Long eventId, UUID userId) {

        User user = userReader.getLoginAllowedUser(userId);

        EventBookmark bookmark = eventBookmarkRepository
                .findByEventIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.BOOKMARK_NOT_FOUND,
                        "이미 찜하지 않은 이벤트입니다."
                ));

        eventBookmarkRepository.delete(bookmark);

        Long bookmarkedEventId = bookmark.getEvent().getId();
        eventRepository.decrementBookmark(bookmarkedEventId);

        hotEventService.decreaseEventScore(bookmarkedEventId, HotEventService.Action.BOOKMARK);

        long count = eventRepository.getBookmarkCount(eventId);

        return new EventBookmarkResponseDto(
                eventId,
                false,
                count
        );
    }

    /*──────────────────────────────────────────────
     | 3. 북마크한 이벤트 목록 조회
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public Page<EventInfoDto> getBookmarkedEvents(UUID userId, String state, Pageable pageable) {

        User user = userReader.getLoginAllowedUser(userId);

        EventStatus statusFilter = null;

        if (state != null) {
            try {
                statusFilter = EventStatus.valueOf(state.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_EVENT_STATUS,
                        "유효하지 않은 이벤트 상태입니다.",
                        "INVALID_EVENT_STATUS_PARAM"
                );
            }
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EventBookmark> bookmarks =
                (statusFilter != null)
                        ? eventBookmarkRepository.findByUserIdAndEvent_Status(user.getId(), statusFilter, sortedPageable)
                        : eventBookmarkRepository.findByUserId(user.getId(), sortedPageable);

        return bookmarks.map(bookmark -> {
            Event event = bookmark.getEvent();
            return eventInfoAssembler.assemble(event, true);
        });
    }
}