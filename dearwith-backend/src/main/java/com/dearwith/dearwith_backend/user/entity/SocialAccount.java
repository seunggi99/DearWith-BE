package com.dearwith.dearwith_backend.user.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseDeletableEntity;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 소셜 로그인 계정 연동 정보
 */
@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocialAccount extends BaseDeletableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String socialId;          // 제공자에서 발급한 고유 ID
    private Instant linkedAt;
}
