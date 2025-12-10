package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.notification.dto.DeviceRegisterRequestDto;
import com.dearwith.dearwith_backend.notification.entity.PushDevice;
import com.dearwith.dearwith_backend.notification.repository.PushDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PushDeviceService {

    private final PushDeviceRepository repository;

    public void registerOrUpdate(DeviceRegisterRequestDto req, UUID userId) {

        PushDevice device = null;

        if (userId != null) {
            device = repository.findByDeviceIdAndUserId(req.deviceId(), userId)
                    .orElse(null);
        }

        if (device == null) {
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
            return;
        }

        // 기존 기기 업데이트
        if (!device.getFcmToken().equals(req.fcmToken())) {
            device.setFcmToken(req.fcmToken());
        }

        device.setPlatform(req.platform());
        device.setPhoneModel(req.phoneModel());
        device.setOsVersion(req.osVersion());
        device.setLastActiveAt(LocalDateTime.now());
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
            }
        }

        // fcmToken 기준 삭제
        if (fcmToken != null && !fcmToken.isBlank()) {
            int cnt = repository.deleteByUserIdAndFcmToken(userId, fcmToken);
            deleted += cnt;
            if (cnt > 0) {
                log.info("[PushDevice] delete by fcmToken: userId={}, fcmToken={}, deleted={}",
                        userId, fcmToken, cnt);
            }
        }

        // ⃣ 두 방식 모두로 삭제된 게 없으면 → 로그만 남김
        if (deleted == 0) {
            log.warn("[PushDevice] unregister: no match. (userId={}, deviceId={}, fcmToken={})",
                    userId, deviceId, fcmToken);
        }
    }
}