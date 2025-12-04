package com.dearwith.dearwith_backend.notification.repository;

import com.dearwith.dearwith_backend.notification.entity.PushDevice;
import com.dearwith.dearwith_backend.notification.enums.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {
    Optional<PushDevice> findByFcmToken(String fcmToken);
    List<PushDevice> findAllByUserId(UUID userId);
    List<PushDevice> findAllByUserIdIn(List<UUID> userIds);
    List<PushDevice> findByUserIdAndPlatformAndDeviceModel(
            UUID userId,
            Platform platform,
            String deviceModel
    );
}
