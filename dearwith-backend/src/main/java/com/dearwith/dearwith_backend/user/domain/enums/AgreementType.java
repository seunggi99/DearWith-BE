package com.dearwith.dearwith_backend.user.domain.enums;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 약관 동의 종류
 */
public enum AgreementType {
    AGE_OVER_14(true),         // [필수] 만 14세 이상
    TERMS_OF_SERVICE(true),    // [필수] 서비스 이용 약관 동의
    PERSONAL_INFORMATION(true),// [필수] 개인정보 수집 및 이용 동의
    MARKETING_CONSENT(false),   // [선택] 마케팅 정보 활용 동의
    PUSH_NOTIFICATION(false);    // [선택] 푸시 알림 수신 동의

    private final boolean required;

    AgreementType(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    @JsonCreator
    public static AgreementType from(String value) {
        try {
            return AgreementType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_AGREEMENT_TYPE);
        }
    }
}
