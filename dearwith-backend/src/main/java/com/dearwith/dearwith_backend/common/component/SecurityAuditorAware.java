package com.dearwith.dearwith_backend.common.component;

import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityAuditorAware implements AuditorAware<User> {

    private final UserRepository userRepository;

    @Override
    public Optional<User> getCurrentAuditor() {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();

        // 인증 안 돼 있으면 비워둠
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        // 익명 인증(비로그인) 무시
        if (authentication.getPrincipal() instanceof String principalStr
                && "anonymousUser".equals(principalStr)) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            UUID userId = userDetails.getId();
            return Optional.of(userRepository.getReferenceById(userId));
        }

        return Optional.empty();
    }
}