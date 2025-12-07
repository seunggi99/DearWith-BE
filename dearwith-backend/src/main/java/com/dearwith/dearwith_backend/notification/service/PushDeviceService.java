package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.notification.dto.DeviceRegisterRequestDto;
import com.dearwith.dearwith_backend.notification.entity.PushDevice;
import com.dearwith.dearwith_backend.notification.repository.PushDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
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


    public void unregister(String fcmToken, UUID userId) {
        if (userId != null) {
            repository.deleteByFcmTokenAndUserId(fcmToken, userId);
        } else {
            repository.deleteByFcmToken(fcmToken);
        }
    }
}