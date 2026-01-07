package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.common.log.BusinessLogService;
import com.dearwith.dearwith_backend.logging.constant.BusinessAction;
import com.dearwith.dearwith_backend.logging.constant.TargetType;
import com.dearwith.dearwith_backend.logging.enums.BusinessLogCategory;
import com.dearwith.dearwith_backend.notification.dto.DeviceRegisterRequestDto;
import com.dearwith.dearwith_backend.notification.entity.PushDevice;
import com.dearwith.dearwith_backend.notification.repository.PushDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PushDeviceService {

    private final PushDeviceRepository repository;
    private final BusinessLogService businessLogService;

    public void registerOrUpdate(DeviceRegisterRequestDto req, UUID userId) {


        PushDevice device = null;

        if (userId != null) {
            device = repository.findByDeviceIdAndUserId(req.deviceId(), userId)
                    .orElse(null);
        }

        boolean isNew = (device == null);

        if (isNew) {
            device = PushDevice.builder()
                    .userId(userId)
                    .deviceId(req.deviceId())
                    .fcmToken(req.fcmToken())
                    .platform(req.platform())
                    .phoneModel(req.phoneModel())
                    .osVersion(req.osVersion())
                    .lastActiveAt(Instant.now())
                    .enabled(true)
                    .disabledAt(null)
                    .disabledReason(null)
                    .build();

            repository.save(device);

        } else {
            if (!device.isEnabled()) {
                device.setEnabled(true);
                device.setDisabledAt(null);
                device.setDisabledReason(null);
            }

            if (device.getFcmToken() == null || !device.getFcmToken().equals(req.fcmToken())) {
                device.setFcmToken(req.fcmToken());
            }

            device.setPlatform(req.platform());
            device.setPhoneModel(req.phoneModel());
            device.setOsVersion(req.osVersion());
            device.setLastActiveAt(Instant.now());
        }

        businessLogService.info(
                BusinessLogCategory.PUSH,
                BusinessAction.Push.PUSH_DEVICE_REGISTER,
                userId,
                TargetType.USER,
                userId != null ? userId.toString() : null,
                isNew ? "푸시 디바이스 최초 등록" : "푸시 디바이스 정보 업데이트",
                Map.of(
                        "deviceId", req.deviceId(),
                        "platform", String.valueOf(req.platform()),
                        "phoneModel", req.phoneModel() != null ? req.phoneModel() : "",
                        "osVersion", req.osVersion() != null ? req.osVersion() : "",
                        "hasFcmToken", String.valueOf(req.fcmToken() != null && !req.fcmToken().isBlank())
                )
        );
    }


    @Transactional
    public void unregister(UUID userId, String deviceId, String fcmToken) {
        if (userId == null) return;

        int disabled = 0;
        Instant now = Instant.now();
        String reason = "USER_UNREGISTER";

        // deviceId 기준 disable
        if (deviceId != null && !deviceId.isBlank()) {
            int cnt = repository.disableByUserIdAndDeviceId(userId, deviceId, now, reason);
            disabled += cnt;

            if (cnt > 0) {
                log.info("[PushDevice] disable by deviceId: userId={}, deviceId={}, disabled={}",
                        userId, deviceId, cnt);

                businessLogService.info(
                        BusinessLogCategory.PUSH,
                        BusinessAction.Push.PUSH_DEVICE_UNREGISTER,
                        userId,
                        TargetType.USER,
                        userId.toString(),
                        "푸시 디바이스 비활성화 (deviceId 기준)",
                        Map.of(
                                "deviceId", deviceId,
                                "disabled", String.valueOf(cnt),
                                "reason", reason
                        )
                );
            }
        }

        // fcmToken 기준 disable
        if (fcmToken != null && !fcmToken.isBlank()) {
            int cnt = repository.disableByUserIdAndFcmToken(userId, fcmToken, now, reason);
            disabled += cnt;

            if (cnt > 0) {
                log.info("[PushDevice] disable by fcmToken: userId={}, disabled={}",
                        userId, cnt);

                businessLogService.info(
                        BusinessLogCategory.PUSH,
                        BusinessAction.Push.PUSH_DEVICE_UNREGISTER,
                        userId,
                        TargetType.USER,
                        userId.toString(),
                        "푸시 디바이스 비활성화 (fcmToken 기준)",
                        Map.of(
                                "hasFcmToken", "true",
                                "disabled", String.valueOf(cnt),
                                "reason", reason
                        )
                );
            }
        }

        if (disabled == 0) {
            log.warn("[PushDevice] unregister: no match. (userId={}, deviceId={}, hasToken={})",
                    userId, deviceId, (fcmToken != null && !fcmToken.isBlank()));

            businessLogService.warn(
                    BusinessLogCategory.PUSH,
                    BusinessAction.Push.PUSH_DEVICE_UNREGISTER,
                    userId,
                    TargetType.USER,
                    userId.toString(),
                    "푸시 디바이스 비활성화 대상 없음",
                    Map.of(
                            "deviceId", deviceId != null ? deviceId : "",
                            "hasFcmToken", String.valueOf(fcmToken != null && !fcmToken.isBlank())
                    )
            );
        }
    }

    @Transactional
    public void unregisterAll(UUID userId) {
        if (userId == null) return;

        Instant now = Instant.now();
        String reason = "USER_UNREGISTER_ALL";

        int cnt = repository.disableAllByUserId(userId, now, reason);

        if (cnt > 0) {
            log.info("[PushDevice] disable all: userId={}, disabled={}", userId, cnt);

            businessLogService.info(
                    BusinessLogCategory.PUSH,
                    BusinessAction.Push.PUSH_DEVICE_UNREGISTER,
                    userId,
                    TargetType.USER,
                    userId.toString(),
                    "푸시 디바이스 전체 비활성화 (userId 기준)",
                    Map.of(
                            "disabled", String.valueOf(cnt),
                            "reason", reason
                    )
            );
        } else {
            businessLogService.warn(
                    BusinessLogCategory.PUSH,
                    BusinessAction.Push.PUSH_DEVICE_UNREGISTER,
                    userId,
                    TargetType.USER,
                    userId.toString(),
                    "푸시 디바이스 전체 비활성화 대상 없음",
                    Map.of()
            );
        }
    }

    private String tokenPrefix(String token) {
        if (token == null) return "";
        return token.length() > 10 ? token.substring(0, 10) : token;
    }
}