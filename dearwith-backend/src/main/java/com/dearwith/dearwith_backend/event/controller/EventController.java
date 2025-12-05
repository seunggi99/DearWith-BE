package com.dearwith.dearwith_backend.event.controller;

import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestBody @Valid EventCreateRequestDto request
    ) {
        return  eventCommandService.create(principal.getId(), request);
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "이벤트 수정",
            description = EventApiDocs.UPDATE_DESC)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestBody @Valid EventUpdateRequestDto request
    ) {
        eventCommandService.update(eventId, principal.getId(), request);
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "이벤트 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        eventCommandService.delete(eventId, principal.getId());
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "이벤트 상세 조회")
    public ResponseEntity<EventResponseDto> getEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        return ResponseEntity.ok(eventQueryService.getEvent(eventId, principal.getId()));
    }

    @GetMapping
    @Operation(summary = "이벤트 검색")
    public Page<EventInfoDto> searchEvents(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(name = "query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("title").ascending());

        return eventQueryService.search(principal.getId(),query, pageable);
    }
}


