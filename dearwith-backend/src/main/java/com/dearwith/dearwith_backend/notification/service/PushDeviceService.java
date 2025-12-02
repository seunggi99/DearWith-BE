package com.dearwith.dearwith_backend.notification.service;

import com.dearwith.dearwith_backend.notification.dto.DeviceRegisterRequestDto;
import com.dearwith.dearwith_backend.notification.entity.PushDevice;
import com.dearwith.dearwith_backend.notification.repository.PushDeviceRepository;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PushDeviceService {

    private final PushDeviceRepository repository;

    public void registerOrUpdate(DeviceRegisterRequestDto req, UUID userId) {

        // 1) 토큰으로 기존 기기 조회
        PushDevice device = repository.findByFcmToken(req.getFcmToken())
                .orElse(null);

        if (device == null) {
            // 2) 신규 등록
            device = PushDevice.builder()
                    .fcmToken(req.getFcmToken())
                    .platform(req.getPlatform())
                    .deviceModel(req.getDeviceModel())
                    .userId(userId)
                    .active(true)
                    .lastActiveAt(LocalDateTime.now())
                    .build();
        } else {
            // 3) 기존 기기 업데이트
            device.setPlatform(req.getPlatform());
            device.setDeviceModel(req.getDeviceModel());
            device.setActive(true);
            device.setLastActiveAt(LocalDateTime.now());

            // 로그인 상태면 userId 덮어씌움 (비로그인 → 로그인 전환)
            if (userId != null) {
                device.setUserId(userId);
            }
        }

        // 4) 저장
        repository.save(device);
    }
}