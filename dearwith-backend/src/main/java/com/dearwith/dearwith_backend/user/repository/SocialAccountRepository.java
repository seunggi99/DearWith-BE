package com.dearwith.dearwith_backend.user.repository;

import com.dearwith.dearwith_backend.user.entity.SocialAccount;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    // 소셜 제공자 + 소셜ID로 유저 조회
    Optional<SocialAccount> findByProviderAndSocialId(AuthProvider provider, String socialId);

    // 중복 연결 방지용 존재 여부 체크
    boolean existsByProviderAndSocialIdAndDeletedAtIsNull(AuthProvider provider, String socialId);

    // 유저 기준으로 모든 소셜 계정 조회
    java.util.List<SocialAccount> findAllByUserId(java.util.UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SocialAccount s
           set s.deletedAt = CURRENT_TIMESTAMP
         where s.user.id = :userId
           and s.deletedAt is null
    """)
    int softDeleteAllByUserId(@Param("userId") UUID userId);
}
