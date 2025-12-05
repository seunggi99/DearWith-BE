package com.dearwith.dearwith_backend.event.controller;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.common.dto.CreatedResponseDto;
import com.dearwith.dearwith_backend.event.dto.EventNoticeListResponseDto;
import com.dearwith.dearwith_backend.event.dto.EventNoticeRequestDto;
import com.dearwith.dearwith_backend.event.dto.EventNoticeResponseDto;
import com.dearwith.dearwith_backend.event.enums.EventNoticeSort;
import com.dearwith.dearwith_backend.event.service.EventNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventNoticeController {
    private final EventNoticeService eventNoticeService;
    @GetMapping("/notices/{noticeId}")
    @Operation(summary = "이벤트 공지 상세 조회")
    public EventNoticeResponseDto getNotice(
            @CurrentUser UUID userId,
            @PathVariable Long noticeId
    ) {
        return eventNoticeService.getNoticeById(noticeId, userId);
    }

    @GetMapping("/{eventId}/notices")
    @Operation(summary = "이벤트 공지 목록 조회")
    public EventNoticeListResponseDto getNotices(
            @CurrentUser UUID userId,
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "LATEST") EventNoticeSort sort
    ) {

        Sort springSort = switch (sort) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
            case MOST_VIEWED -> Sort.by(Sort.Direction.DESC, "viewCount")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        };

        Pageable pageable = PageRequest.of(page, size, springSort);

        return eventNoticeService.getNoticesByEvent(eventId, userId, pageable);
    }

    @PostMapping("/{eventId}/notices")
    @Operation(summary = "이벤트 공지 등록")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedResponseDto createNotice(
            @CurrentUser UUID userId,
            @PathVariable Long eventId,
            @RequestBody @Valid EventNoticeRequestDto req
    ) {
        return eventNoticeService.create(userId, eventId, req);
    }

    @PatchMapping("/{eventId}/notices/{noticeId}")
    @Operation(summary = "이벤트 공지 수정")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateNotice(
            @CurrentUser UUID userId,
            @PathVariable Long eventId,
            @PathVariable Long noticeId,
            @RequestBody @Valid EventNoticeRequestDto req
    ) {
        eventNoticeService.update(userId, eventId, noticeId, req);
    }

    @DeleteMapping("/{eventId}/notices/{noticeId}")
    @Operation(summary = "이벤트 공지 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotice(
            @CurrentUser UUID userId,
            @PathVariable Long eventId,
            @PathVariable Long noticeId
    ) {
        eventNoticeService.delete(userId, eventId, noticeId);
    }
}
