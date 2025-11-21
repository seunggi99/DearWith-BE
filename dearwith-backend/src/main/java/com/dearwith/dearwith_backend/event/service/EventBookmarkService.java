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
import com.dearwith.dearwith_backend.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final HotEventService hotEventService;
    private final EventInfoAssembler eventInfoAssembler;

    @Transactional
    public EventBookmarkResponseDto addBookmark(Long eventId, UUID userId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        try {
            eventBookmarkRepository.save(EventBookmark.builder()
                    .event(event).user(user).build());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_BOOKMARKED_EVENT);
        }

        eventRepository.incrementBookmark(eventId);

        hotEventService.increaseEventScore(eventId, HotEventService.Action.BOOKMARK);


        long count = eventRepository.getBookmarkCount(eventId);

        return new EventBookmarkResponseDto(
                eventId,
                true,
                count
        );
    }

    @Transactional
    public EventBookmarkResponseDto removeBookmark(Long eventId, UUID userId) {
        EventBookmark bookmark = eventBookmarkRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKMARK_NOT_FOUND));

        eventBookmarkRepository.delete(bookmark);
        eventRepository.decrementBookmark(bookmark.getEvent().getId());

        hotEventService.decreaseEventScore(bookmark.getEvent().getId(), HotEventService.Action.BOOKMARK);

        long count = eventRepository.getBookmarkCount(eventId);

        return new EventBookmarkResponseDto(
                eventId,
                false,
                count
        );
    }

    @Transactional(readOnly = true)
    public Page<EventInfoDto> getBookmarkedEvents(UUID userId, String state, Pageable pageable) {

        EventStatus statusFilter = null;

        if (state != null) {
            try {
                statusFilter = EventStatus.valueOf(state.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_EVENT_STATUS);
            }
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EventBookmark> bookmarks =
                (statusFilter != null)
                        ? eventBookmarkRepository.findByUserIdAndEvent_Status(userId, statusFilter, sortedPageable)
                        : eventBookmarkRepository.findByUserId(userId, sortedPageable);

        return bookmarks.map(bookmark -> {
            Event event = bookmark.getEvent();
            return eventInfoAssembler.assemble(event, true);
        });
    }
}