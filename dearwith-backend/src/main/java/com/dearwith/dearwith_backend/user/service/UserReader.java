package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserReader {

    private final UserRepository userRepository;
    private final UserGuard userGuard;

    /*──────────────────────────────
     | 1) 기본 조회 (상태와 무관)
     | - 탈퇴/정지/작성제한 상관없이 엔티티만 필요할 때
     | - ex: 리뷰 상세에서 작성자 닉네임 표시 등
     *──────────────────────────────*/
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        BusinessException.withMessage(
                                ErrorCode.NOT_FOUND,
                                "사용자를 찾을 수 없습니다."
                        )
                );
    }

    /*──────────────────────────────
     | 2) 로그인 확인
     | - “로그인한 사용자만 접근 가능”
     | - 조회 기능은 가능, 작성제한은 무시
     | - ex: 마이페이지, 내 북마크, 내 정보 보기
     *──────────────────────────────*/
    public User getLoginAllowedUser(UUID userId) {
        User user = getUser(userId);
        userGuard.ensureLoginAllowed(user);
        return user;
    }

    public List<User> getLoginAllowedUsers(List<UUID> userIds) {

        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<User> users = userRepository.findAllById(userIds);

        for (User user : users) {
            userGuard.ensureLoginAllowed(user);
        }

        return users;
    }

    public List<User> getAllLoginAllowedUsers() {
        return userRepository.findAllByUserStatusLoginAllowed();
    }


    /*──────────────────────────────
     | 3) 완전 ACTIVE 유저만 (탈퇴/정지/작성제한 모두 차단)
     *──────────────────────────────*/
    public User getActiveUser(UUID userId) {
        User user = getUser(userId);
        userGuard.ensureActive(user);
        return user;
    }
}
