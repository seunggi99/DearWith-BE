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

import java.time.LocalDateTime;
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
                    .lastActiveAt(LocalDateTime.now())
                    .build();

            repository.save(device);
        } else {
            // 기존 기기 업데이트
            if (device.getFcmToken() == null || !device.getFcmToken().equals(req.fcmToken())) {
                device.setFcmToken(req.fcmToken());
            }

            device.setPlatform(req.platform());
            device.setPhoneModel(req.phoneModel());
            device.setOsVersion(req.osVersion());
            device.setLastActiveAt(LocalDateTime.now());
        }

        businessLogService.info(
                BusinessLogCategory.PUSH,
                BusinessAction.Push.PUSH_DEVICE_REGISTER,
                userId,                                        // actor: 이 디바이스를 등록한 유저
                TargetType.USER,
                userId != null ? userId.toString() : null,    // target: 해당 유저
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

        int deleted = 0;

        // deviceId 기준 삭제
        if (deviceId != null && !deviceId.isBlank()) {
            int cnt = repository.deleteByUserIdAndDeviceId(userId, deviceId);
            deleted += cnt;
            if (cnt > 0) {
                log.info("[PushDevice] delete by deviceId: userId={}, deviceId={}, deleted={}",
                        userId, deviceId, cnt);

                businessLogService.info(
                        BusinessLogCategory.PUSH,
                        BusinessAction.Push.PUSH_DEVICE_UNREGISTER,
                        userId,
                        TargetType.USER,
                        userId != null ? userId.toString() : null,
                        "푸시 디바이스 해제 (deviceId 기준)",
                        Map.of(
                                "deviceId", deviceId,
                                "deleted", String.valueOf(cnt)
                        )
                );
            }
        }

        // fcmToken 기준 삭제
        if (fcmToken != null && !fcmToken.isBlank()) {
            int cnt = repository.deleteByUserIdAndFcmToken(userId, fcmToken);
            deleted += cnt;
            if (cnt > 0) {
                log.info("[PushDevice] delete by fcmToken: userId={}, fcmToken={}, deleted={}",
                        userId, fcmToken, cnt);

                businessLogService.info(
                        BusinessLogCategory.PUSH,
                        BusinessAction.Push.PUSH_DEVICE_UNREGISTER,
                        userId,
                        TargetType.USER,
                        userId != null ? userId.toString() : null,
                        "푸시 디바이스 해제 (fcmToken 기준)",
                        Map.of(
                                "hasFcmToken", "true",
                                "deleted", String.valueOf(cnt)
                        )
                );
            }
        }

        // ⃣ 두 방식 모두로 삭제된 게 없으면 → 단순 경고 + 비즈니스 경고
        if (deleted == 0) {
            log.warn("[PushDevice] unregister: no match. (userId={}, deviceId={}, fcmToken={})",
                    userId, deviceId, fcmToken);

            businessLogService.warn(
                    BusinessLogCategory.PUSH,
                    BusinessAction.Push.PUSH_DEVICE_UNREGISTER,
                    userId,
                    TargetType.USER,
                    userId != null ? userId.toString() : null,
                    "푸시 디바이스 해제 대상 없음",
                    Map.of(
                            "deviceId", deviceId != null ? deviceId : "",
                            "hasFcmToken", String.valueOf(fcmToken != null && !fcmToken.isBlank())
                    )
            );
        }
    }
}