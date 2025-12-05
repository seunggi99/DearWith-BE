package com.dearwith.dearwith_backend.event.controller;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.common.dto.CreatedResponseDto;
import com.dearwith.dearwith_backend.event.docs.EventApiDocs;
import com.dearwith.dearwith_backend.event.dto.*;
import com.dearwith.dearwith_backend.event.service.EventCommandService;
import com.dearwith.dearwith_backend.event.service.EventQueryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedResponseDto createEvent(
            @CurrentUser UUID userId,
            @RequestBody @Valid EventCreateRequestDto request
    ) {
        return  eventCommandService.create(userId, request);
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "이벤트 수정",
            description = EventApiDocs.UPDATE_DESC)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEvent(
            @PathVariable Long eventId,
            @CurrentUser UUID userId,
            @RequestBody @Valid EventUpdateRequestDto request
    ) {
        eventCommandService.update(eventId, userId, request);
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "이벤트 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(
            @PathVariable Long eventId,
            @CurrentUser UUID userId
    ) {
        eventCommandService.delete(eventId, userId);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "이벤트 상세 조회")
    public ResponseEntity<EventResponseDto> getEvent(
            @PathVariable Long eventId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(eventQueryService.getEvent(eventId, userId));
    }

    @GetMapping
    @Operation(summary = "이벤트 검색")
    public Page<EventInfoDto> searchEvents(
            @CurrentUser UUID userId,
            @RequestParam(name = "query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("title").ascending());

        return eventQueryService.search(userId,query, pageable);
    }
}


