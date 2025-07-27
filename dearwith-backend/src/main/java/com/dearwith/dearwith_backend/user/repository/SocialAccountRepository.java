package com.dearwith.dearwith_backend.user.repository;

import com.dearwith.dearwith_backend.user.entity.SocialAccount;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    // 소셜 제공자 + 소셜ID로 유저 조회 (로그인 시 사용)
    Optional<SocialAccount> findByProviderAndSocialId(AuthProvider provider, String socialId);

    // 중복 연결 방지용 존재 여부 체크
    boolean existsByProviderAndSocialId(AuthProvider provider, String socialId);

    // 유저 기준으로 모든 소셜 계정 조회 (마이페이지 연결 계정 보기 등)
    java.util.List<SocialAccount> findAllByUserId(java.util.UUID userId);
}
