package com.dearwith.dearwith_backend.user.entity;

import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 소셜 로그인 계정 연동 정보
 */
@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocialAccount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider; // KAKAO, APPLE, GOOGLE 등 // 소셜 제공자

    private String socialId;          // 제공자에서 발급한 고유 ID
    private LocalDateTime linkedAt;   // 연동 시각
}
