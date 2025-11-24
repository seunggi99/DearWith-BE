package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.entity.SocialAccount;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import com.dearwith.dearwith_backend.user.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialAccountService {

    private final SocialAccountRepository socialAccountRepository;

    public Optional<SocialAccount> findByProviderAndSocialId(AuthProvider provider, String socialId) {

        if (provider == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "소셜 로그인 제공자가 유효하지 않습니다.",
                    "OAUTH_PROVIDER_NULL"
            );
        }

        if (socialId == null || socialId.isBlank()) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "소셜 로그인 인증에 실패했습니다.",
                    "SOCIAL_ID_NULL_OR_BLANK"
            );
        }

        return socialAccountRepository.findByProviderAndSocialId(provider, socialId);
    }

    public void connectSocialAccount(User user, AuthProvider provider, String socialId) {

        if (provider == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "소셜 로그인 제공자가 유효하지 않습니다.",
                    "OAUTH_PROVIDER_NULL"
            );
        }

        if (socialId == null || socialId.isBlank()) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "소셜 로그인 인증에 실패했습니다.",
                    "SOCIAL_ID_NULL_OR_BLANK"
            );
        }

        boolean exists = socialAccountRepository.existsByProviderAndSocialId(provider, socialId);
        if (exists) {
            // 사용자용 메시지는 간결하게
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.DUPLICATE_SOCIAL_ACCOUNT,
                    ErrorCode.DUPLICATE_SOCIAL_ACCOUNT.getMessage(),
                    "SOCIAL_ACCOUNT_ALREADY_LINKED:" + provider + ":" + socialId
            );
        }

        SocialAccount account = SocialAccount.builder()
                .user(user)
                .provider(provider)
                .socialId(socialId)
                .linkedAt(LocalDateTime.now())
                .build();

        socialAccountRepository.save(account);
    }
}