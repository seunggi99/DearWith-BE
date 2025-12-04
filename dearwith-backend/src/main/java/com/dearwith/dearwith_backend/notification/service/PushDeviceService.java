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

        // 1) 토큰으로 이미 존재하면 그대로 업데이트
        PushDevice device = repository.findByFcmToken(req.getFcmToken())
                .orElse(null);

        if (device == null) {

            // 2) 같은 기기(= userId + platform + deviceModel) row는 전부 삭제
            if (userId != null) {
                List<PushDevice> duplicates =
                        repository.findByUserIdAndPlatformAndDeviceModel(
                                userId,
                                req.getPlatform(),
                                req.getDeviceModel()
                        );

                duplicates.forEach(repository::delete);
            }

            // 3) 신규 등록
            device = PushDevice.builder()
                    .fcmToken(req.getFcmToken())
                    .platform(req.getPlatform())
                    .deviceModel(req.getDeviceModel())
                    .userId(userId)
                    .lastActiveAt(LocalDateTime.now())
                    .build();

            repository.save(device);
        } else {
            // 4) 기존 토큰 row 업데이트
            device.setPlatform(req.getPlatform());
            device.setDeviceModel(req.getDeviceModel());
            if (userId != null) device.setUserId(userId);
            device.setLastActiveAt(LocalDateTime.now());
        }
    }
}