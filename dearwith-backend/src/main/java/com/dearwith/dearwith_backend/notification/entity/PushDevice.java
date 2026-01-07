package com.dearwith.dearwith_backend.notification.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseDeletableEntity;
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
public class PushDevice extends BaseDeletableEntity {

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

    /** 전송 가능 여부 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 비활성화 시점 */
    @Column(nullable = true)
    private Instant disabledAt;

    /** 비활성화 사유 */
    @Column(nullable = true, length = 50)
    private String disabledReason;

    public void disable(String reason) {
        this.enabled = false;
        this.disabledAt = Instant.now();
        this.disabledReason = reason;
    }

    public void enable() {
        this.enabled = true;
        this.disabledAt = null;
        this.disabledReason = null;
    }
}