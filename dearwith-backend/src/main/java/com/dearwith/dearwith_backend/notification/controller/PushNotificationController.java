package com.dearwith.dearwith_backend.notification.controller;

import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.notification.dto.DeviceRegisterRequestDto;
import com.dearwith.dearwith_backend.notification.service.PushDeviceService;
import com.dearwith.dearwith_backend.notification.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.io.IOException;
import java.util.UUID;


@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushNotificationController {
    private final PushDeviceService pushDeviceService;
    private final PushNotificationService pushNotificationService;


    @Operation(summary = "Fcm 기기 등록")
    @PostMapping("/devices")
    public void registerDevice(
            @RequestBody DeviceRegisterRequestDto req,
            @AuthenticationPrincipal Object principal
    ) {
        UUID userId = null;

        if (principal instanceof CustomUserDetails cud) {
            userId = cud.getId();
        }

        pushDeviceService.registerOrUpdate(req, userId);
    }

    @Operation(summary = "테스트용 토큰 푸시 api")
    @GetMapping("/test")
    public String test(@RequestParam String token) throws IOException {
        pushNotificationService.sendToToken(token, "테스트 푸시", "푸시가 잘 오나요?","http://192.168.219.106:3000/push");
        return "OK";
    }

    @Operation(summary = "테스트용 유저 푸시 api")
    @GetMapping("/push/test/me")
    public String testMe(@AuthenticationPrincipal(expression = "id") UUID userId) {
        pushNotificationService.sendToUser(userId, "테스트 푸시", "내 계정으로 오는지 테스트","http://192.168.219.106:3000/push");
        return "OK";
    }
}
