package com.dearwith.dearwith_backend.event.controller;

import com.dearwith.dearwith_backend.event.dto.EventBookmarkResponseDto;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.service.EventBookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.dearwith.dearwith_backend.event.docs.EventApiDocs.READ_BOOKMARK_DESC;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventBookmarkController {

    private final EventBookmarkService eventBookmarkService;

    @Operation(summary = "북마크힌 이벤트 조회",
            description = READ_BOOKMARK_DESC)
    @GetMapping("/bookmark")
    public Page<EventInfoDto> getBookmarkedEvents(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestParam String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return eventBookmarkService.getBookmarkedEvents(userId, state, pageable);
    }

    @Operation(summary = "북마크 추가")
    @PostMapping("/{eventId}/bookmark")
    public EventBookmarkResponseDto addBookmark(
            @PathVariable Long eventId,
            @AuthenticationPrincipal(expression = "id") UUID userId
    ) {
        return eventBookmarkService.addBookmark(eventId, userId);
    }

    @Operation(summary = "북마크 해제")
    @DeleteMapping("/{eventId}/bookmark")
    public EventBookmarkResponseDto removeBookmark(
            @PathVariable Long eventId,
            @AuthenticationPrincipal(expression = "id") UUID userId
    ) {
        return eventBookmarkService.removeBookmark(eventId, userId);
    }

}
