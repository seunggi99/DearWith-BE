package com.dearwith.dearwith_backend.notification.entity;

import com.dearwith.dearwith_backend.notification.enums.Platform;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class PushDevice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 255)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Column(nullable = true, length = 100)
    private String deviceModel;

    @Column(nullable = false)
    private LocalDateTime lastActiveAt = LocalDateTime.now();
}