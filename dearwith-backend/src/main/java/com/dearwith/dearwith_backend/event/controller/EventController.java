package com.dearwith.dearwith_backend.event.controller;

import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @Operation(summary = "북마크 해제")
    @DeleteMapping("/{eventId}/bookmark")
    public ResponseEntity<Void> removeBookmark(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        eventService.removeBookmark(eventId, principal.getId());
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "북마크 추가")
    @PostMapping("/{eventId}/bookmark")
    public ResponseEntity<Void> addBookmark(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        eventService.addBookmark(eventId, principal.getId());
        return ResponseEntity.ok().build();
    }
}
