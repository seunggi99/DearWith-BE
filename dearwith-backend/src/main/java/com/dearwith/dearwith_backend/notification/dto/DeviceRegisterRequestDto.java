package com.dearwith.dearwith_backend.notification.dto;

import com.dearwith.dearwith_backend.notification.enums.Platform;

public record DeviceRegisterRequestDto(
        String deviceId,
        String fcmToken,
        Platform platform,
        String phoneModel,
        String osVersion
) {}