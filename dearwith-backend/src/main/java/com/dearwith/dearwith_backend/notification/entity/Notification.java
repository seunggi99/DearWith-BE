package com.dearwith.dearwith_backend.notification.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseTimeEntity;
import com.dearwith.dearwith_backend.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 300)
    private String linkUrl;

    private Long targetId;

    @Column(name = "is_read")
    private boolean read;

    private LocalDateTime readAt;
}
