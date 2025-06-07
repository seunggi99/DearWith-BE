package com.dearwith.dearwith_backend.user.domain;

import com.dearwith.dearwith_backend.user.domain.enums.AuthProvider;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 소셜 로그인 계정 연동 정보
 */
@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocialAccount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;            // User.id 참조
    private AuthProvider provider;    // 소셜 제공자
    private String socialId;          // 제공자에서 발급한 고유 ID
    private LocalDateTime linkedAt;   // 연동 시각
}
