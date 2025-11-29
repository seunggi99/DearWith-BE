package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserGuard {

    /*────────────────────────────
     | 공통 삭제(탈퇴) 체크
     *────────────────────────────*/
    private void checkDeleted(User user) {
        if (user.getDeletedAt() != null) {
            throw BusinessException.of(ErrorCode.USER_DELETED);
        }
    }


    /*────────────────────────────
     | 활성 여부 (ACTIVE만 통과)
     | - 단순 ACTIVE 체크만 필요할 때 사용
     *────────────────────────────*/
    public void ensureActive(User user) {
        checkDeleted(user);

        if (user.getUserStatus() == UserStatus.SUSPENDED) {
            throw buildSuspendedException(user);
        }
        if (user.getUserStatus() == UserStatus.WRITE_RESTRICTED) {
            throw buildWriteRestrictedException(user);
        }
    }


    /*────────────────────────────
     | 로그인 가능 여부
     | - 로그인, 토큰 인증, 사용자 액션 기반 API
     *────────────────────────────*/
    public void ensureLoginAllowed(User user) {
        checkDeleted(user);
        if (user.getUserStatus() == UserStatus.SUSPENDED) {
            throw buildSuspendedException(user);
        }
    }


    /*────────────────────────────
     | 예외 빌더들 — 사유/기한 포함
     *────────────────────────────*/
    private BusinessException buildSuspendedException(User user) {

        String reason = user.getSuspendedReason();
        LocalDate until = user.getSuspendedUntil();

        // 사용자에게 보여줄 메시지
        String message = "정지된 계정입니다.";

        if (reason != null) {
            message += " 사유: " + reason + ".";
        }
        if (until != null) {
            message += " 정지 해제 예정일: " + until + ".";
        }

        // 프론트에서 구조적으로 파싱하기 쉬운 detail(JSON)
        Map<String, Object> detail = Map.of(
                "reason", reason,
                "until", until
        );

        return BusinessException.withDetailMap(
                ErrorCode.USER_SUSPENDED,
                message,
                detail
        );
    }


    private BusinessException buildWriteRestrictedException(User user) {

        String reason = user.getSuspendedReason();
        LocalDate until = user.getSuspendedUntil();

        String message = "작성 제한 상태입니다.";
        if (reason != null) {
            message += " 사유: " + reason + ".";
        }
        if (until != null) {
            message += " 제한 해제 예정일: " + until + ".";
        }

        Map<String, Object> detail = Map.of(
                "reason", reason,
                "until", until
        );

        return BusinessException.withDetailMap((
                ErrorCode.USER_WRITE_RESTRICTED),
                message,
                detail
        );
    }
}