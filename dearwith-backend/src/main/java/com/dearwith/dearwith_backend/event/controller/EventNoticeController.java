package com.dearwith.dearwith_backend.event.controller;

import com.dearwith.dearwith_backend.event.dto.EventNoticeRequestDto;
import com.dearwith.dearwith_backend.event.dto.EventNoticeResponseDto;
import com.dearwith.dearwith_backend.event.service.EventNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/notices")
@RequiredArgsConstructor
public class EventNoticeController {
    private final EventNoticeService eventNoticeService;

    @GetMapping
    @Operation(summary = "이벤트 공지 목록 조회")
    public Page<EventNoticeResponseDto> getNotices(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return eventNoticeService.getNoticesByEvent(eventId, pageable);
    }

    @PostMapping
    @Operation(summary = "이벤트 공지 등록")
    public EventNoticeResponseDto createNotice(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @PathVariable Long eventId,
            @RequestBody @Valid EventNoticeRequestDto req
    ) {
        return eventNoticeService.create(userId, eventId, req);
    }

    @PatchMapping("/{noticeId}")
    @Operation(summary = "이벤트 공지 수정")
    public EventNoticeResponseDto updateNotice(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @PathVariable Long eventId,
            @PathVariable Long noticeId,
            @RequestBody @Valid EventNoticeRequestDto req
    ) {
        return eventNoticeService.update(userId, eventId, noticeId, req);
    }

    @DeleteMapping("/{noticeId}")
    @Operation(summary = "이벤트 공지 삭제")
    public ResponseEntity<Void> deleteNotice(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @PathVariable Long eventId,
            @PathVariable Long noticeId
    ) {
        eventNoticeService.delete(userId, eventId, noticeId);
        return ResponseEntity.ok().build();
    }
}
