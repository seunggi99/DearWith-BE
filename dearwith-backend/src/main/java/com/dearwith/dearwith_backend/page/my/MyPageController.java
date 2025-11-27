package com.dearwith.dearwith_backend.page.my;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService myPageService;

    @Operation(summary = "마이페이지 조회" )
    @GetMapping
    public MyPageResponseDto getMyPage(@AuthenticationPrincipal(expression = "id", errorOnInvalidType = false) UUID userId
    ){
        MyPageResponseDto response = myPageService.getMyPage(userId);
        return response;
    }

    @Operation(summary = "이벤트 알림 설정 변경")
    @PatchMapping("/notifications/event")
    public boolean updateEventNotification(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestBody NotificationToggleRequestDto request
    ) {
        return myPageService.updateEventNotification(userId, request.isEnabled());
    }

    @Operation(summary = "서비스 알림 설정 변경")
    @PatchMapping("/notifications/service")
    public boolean updateServiceNotification(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestBody NotificationToggleRequestDto request
    ) {
        return myPageService.updateServiceNotification(userId, request.isEnabled());
    }
}
