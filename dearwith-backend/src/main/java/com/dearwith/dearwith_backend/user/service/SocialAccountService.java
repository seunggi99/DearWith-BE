package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.domain.SocialAccount;
import com.dearwith.dearwith_backend.user.domain.User;
import com.dearwith.dearwith_backend.user.domain.enums.AuthProvider;
import com.dearwith.dearwith_backend.user.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialAccountService {
    private final SocialAccountRepository socialAccountRepository;

    public Optional<SocialAccount> findByProviderAndSocialId(AuthProvider provider, String socialId) {
        return socialAccountRepository.findByProviderAndSocialId(provider, socialId);
    }

    public void connectSocialAccount(User user, AuthProvider provider, String socialId) {
        boolean exists = socialAccountRepository.existsByProviderAndSocialId(provider, socialId);
        if (exists) {
            throw new BusinessException(ErrorCode.DUPLICATE_SOCIAL_ACCOUNT);
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
