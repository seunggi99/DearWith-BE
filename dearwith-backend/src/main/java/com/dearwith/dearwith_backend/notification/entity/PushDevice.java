package com.dearwith.dearwith_backend.notification.entity;

import com.dearwith.dearwith_backend.notification.enums.Platform;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class PushDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private UUID userId;

    /**  기기 고유 ID  */
    @Column(nullable = false, length = 100)
    private String deviceId;

    /**  FCM 토큰  **/
    @Column(nullable = false, length = 255)
    private String fcmToken;

    /** iOS / ANDROID / WEB */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    /**  기종명 (예: iPhone 15 Pro, Galaxy S24) */
    @Column(nullable = true, length = 100)
    private String phoneModel;

    /** OS 버전 (예: iOS 17.4, Android 14) */
    @Column(nullable = true, length = 50)
    private String osVersion;

    /** 마지막 활성화 시간 */
    @Column(nullable = false)
    private Instant lastActiveAt = Instant.now();
}