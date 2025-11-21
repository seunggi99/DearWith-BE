package com.dearwith.dearwith_backend.event.controller;

import com.dearwith.dearwith_backend.event.docs.EventApiDocs;
import com.dearwith.dearwith_backend.event.dto.*;
import com.dearwith.dearwith_backend.event.service.EventBookmarkService;
import com.dearwith.dearwith_backend.event.service.EventCommandService;
import com.dearwith.dearwith_backend.event.service.EventQueryService;
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

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventQueryService eventQueryService;
    private final EventCommandService eventCommandService;

    @PostMapping
    @Operation(summary = "이벤트 등록",
            description = EventApiDocs.CREATE_DESC)
    public ResponseEntity<EventResponseDto> createEvent(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestBody @Valid EventCreateRequestDto request
    ) {
        EventResponseDto response = eventCommandService.create(userId, request);
        return ResponseEntity
                .created(URI.create("/api/events/" + response.id()))
                .body(response);
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "이벤트 수정",
            description = EventApiDocs.UPDATE_DESC)
    public ResponseEntity<EventResponseDto> updateEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestBody @Valid EventUpdateRequestDto request
    ) {
        EventResponseDto response = eventCommandService.update(eventId, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "이벤트 삭제")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal(expression = "id") UUID userId
    ) {
        eventCommandService.delete(eventId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "이벤트 상세 조회")
    public ResponseEntity<EventResponseDto> getEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal(expression = "id", errorOnInvalidType = false) UUID userId
    ) {
        return ResponseEntity.ok(eventQueryService.getEvent(eventId, userId));
    }

    @GetMapping
    @Operation(summary = "이벤트 검색")
    public Page<EventInfoDto> searchEvents(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestParam(name = "query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("title").ascending());

        return eventQueryService.search(userId,query, pageable);
    }
}


