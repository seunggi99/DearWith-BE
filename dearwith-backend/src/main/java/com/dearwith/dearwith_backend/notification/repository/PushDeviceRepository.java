package com.dearwith.dearwith_backend.notification.repository;

import com.dearwith.dearwith_backend.notification.entity.PushDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {
    List<PushDevice> findAllByUserId(UUID userId);
    List<PushDevice> findAllByUserIdIn(List<UUID> userIds);
    Optional<PushDevice> findByDeviceIdAndUserId(String deviceId, UUID userId);
    int deleteByUserIdAndFcmToken(UUID userId, String fcmToken);
    int deleteByUserIdAndDeviceId(UUID userId, String deviceId);
}
