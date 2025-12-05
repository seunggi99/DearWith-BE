package com.dearwith.dearwith_backend.notification.controller;

import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.notification.dto.NotificationResponseDto;
import com.dearwith.dearwith_backend.notification.dto.UnreadExistsResponseDto;
import com.dearwith.dearwith_backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/unread-exists")
    @Operation(summary = "안 읽은 알림 존재 여부 및 개수 조회")
    public UnreadExistsResponseDto hasUnread(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        return notificationService.hasUnread(principal.getId());
    }

    @GetMapping
    @Operation(summary = "알림 목록 조회",
            description = """
                    현재 로그인한 사용자의 알림 목록을 페이징하여 반환합니다.
                    - onlyUnread=true 이면 읽지 않은 알림만 조회합니다.
                    """)
    public Page<NotificationResponseDto> getNotifications(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(name = "onlyUnread", defaultValue = "false") boolean onlyUnread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return notificationService.getNotifications(principal.getId(), onlyUnread, pageable);
    }

    // 3) 단일 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "단일 알림 읽음 처리")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(principal.getId(), notificationId);
    }

    // 4) 전체 알림 읽음 처리
    @PatchMapping("/read-all")
    @Operation(summary = "전체 알림 읽음 처리")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        notificationService.markAllAsRead(principal.getId());
    }

    // 5) 단일 알림 삭제
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "단일 알림 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOne(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @PathVariable Long notificationId
    ) {
        notificationService.deleteOne(principal.getId(), notificationId);
    }

    // 6) 전체 알림 삭제 (옵션: 읽은 것만 삭제)
    @DeleteMapping
    @Operation(summary = "전체 or 읽은 알림 삭제",
            description = """
                    현재 로그인한 사용자의 알림을 전체 삭제합니다.
                    - onlyRead=true 이면 '읽은 알림'만 삭제합니다.
                    - onlyRead=false 이면 모든 알림을 삭제합니다.
                    """)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(name = "onlyRead", defaultValue = "false") boolean onlyRead
    ) {
        notificationService.deleteAll(principal.getId(), onlyRead);
    }
}
