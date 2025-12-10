package com.dearwith.dearwith_backend.auth.dto;

public record LogoutRequestDto (
        String deviceId,
        String fcmToken
){ }
