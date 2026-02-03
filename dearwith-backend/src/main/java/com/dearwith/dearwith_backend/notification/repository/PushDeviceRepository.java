package com.dearwith.dearwith_backend.notification.repository;

import com.dearwith.dearwith_backend.notification.entity.PushDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {


    Optional<PushDevice> findByDeviceIdAndUserId(String deviceId, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PushDevice d
           set d.enabled = false,
               d.disabledAt = :disabledAt,
               d.disabledReason = :reason
         where d.fcmToken = :fcmToken
           and d.enabled = true
    """)
    int disableAllByFcmToken(String fcmToken, Instant disabledAt, String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PushDevice d
           set d.enabled = true,
               d.disabledAt = null,
               d.disabledReason = null
         where d.id = :id
    """)
    int enableById(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PushDevice d
           set d.enabled = false,
               d.disabledAt = :disabledAt,
               d.disabledReason = :reason
         where d.userId = :userId
           and d.deviceId = :deviceId
           and d.enabled = true
    """)
    int disableByUserIdAndDeviceId(UUID userId, String deviceId, Instant disabledAt, String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PushDevice d
           set d.enabled = false,
               d.disabledAt = :disabledAt,
               d.disabledReason = :reason
         where d.userId = :userId
           and d.fcmToken = :fcmToken
           and d.enabled = true
    """)
    int disableByUserIdAndFcmToken(UUID userId, String fcmToken, Instant disabledAt, String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PushDevice d
           set d.enabled = false,
               d.disabledAt = :disabledAt,
               d.disabledReason = :reason
         where d.userId = :userId
           and d.enabled = true
    """)
    int disableAllByUserId(UUID userId, Instant disabledAt, String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PushDevice d
           set d.deletedAt = :now
         where d.enabled = false
           and d.disabledAt < :disabledBefore
           and d.deletedAt is null
    """)
    int softDeleteDisabledBefore(
            @Param("disabledBefore") Instant disabledBefore,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
        delete from PushDevice d
         where d.deletedAt < :deletedBefore
    """)
    int hardDeleteDeletedBefore(
            @Param("deletedBefore") Instant deletedBefore
    );

    @Query("""
        SELECT DISTINCT d.fcmToken
        FROM PushDevice d
        WHERE d.userId = :userId
          AND d.enabled = true
          AND d.fcmToken IS NOT NULL
          AND d.fcmToken <> ''
          AND d.lastActiveAt IS NOT NULL
          AND d.lastActiveAt > :expireThreshold
    """)
    List<String> findActiveTokensByUserId(
            @Param("userId") UUID userId,
            @Param("expireThreshold") Instant expireThreshold
    );

    @Query("""
        SELECT DISTINCT d.fcmToken
        FROM PushDevice d
        WHERE d.userId IN :userIds
          AND d.enabled = true
          AND d.fcmToken IS NOT NULL
          AND d.fcmToken <> ''
          AND d.lastActiveAt IS NOT NULL
          AND d.lastActiveAt > :expireThreshold
    """)
    List<String> findActiveTokensByUserIds(
            @Param("userIds") List<UUID> userIds,
            @Param("expireThreshold") Instant expireThreshold
    );
}
