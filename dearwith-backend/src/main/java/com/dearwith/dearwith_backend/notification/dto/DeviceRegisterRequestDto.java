package com.dearwith.dearwith_backend.notification.dto;

import com.dearwith.dearwith_backend.notification.enums.Platform;
import lombok.Getter;

@Getter
public class DeviceRegisterRequestDto {
    private String fcmToken;
    private Platform platform;
    private String deviceModel;
}
