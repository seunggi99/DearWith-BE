package com.dearwith.dearwith_backend.notification.dto;

/**
 * FCM 전송 실패 분류 결과
 *
 * shouldDisable 토큰을 비활성화해야 하는가
 * disableReason 비활성화 사유 (shouldDisable=true일 때만 유효)
 * shouldRetry 재시도 가능한가
 * retryReason 재시도 사유 (shouldRetry=true일 때만 유효)
 */
public record FailureDecision(
        boolean shouldDisable,
        String disableReason,
        boolean shouldRetry,
        String retryReason
) {
    public static FailureDecision disable(String reason) {
        return new FailureDecision(true, reason, false, null);
    }

    public static FailureDecision retry(String reason) {
        return new FailureDecision(false, null, true, reason);
    }

    public static FailureDecision ignore(String reason) {
        return new FailureDecision(false, null, false, reason);
    }
}
