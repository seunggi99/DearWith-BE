package com.dearwith.dearwith_backend.user.dto;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.enums.WithdrawalReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserWithdrawRequestDto(
        @NotNull WithdrawalReason reason,
        @Size(min = 2, message = "사유는 2자 이상 입력해주세요.")
        String detail
) {
    public void validate() {
        if (reason == WithdrawalReason.OTHER) {
            if (detail == null || detail.trim().length() < 2) {
                throw BusinessException.withMessage(
                        ErrorCode.INVALID_INPUT,
                        "사유를 2자 이상 입력해주세요."
                );
            }
        } else {
            if (detail != null && !detail.trim().isEmpty() && detail.trim().length() < 2) {
                throw BusinessException.withMessage(
                        ErrorCode.INVALID_INPUT,
                        "사유는 2자 이상 입력해주세요."
                );
            }
        }
    }
}