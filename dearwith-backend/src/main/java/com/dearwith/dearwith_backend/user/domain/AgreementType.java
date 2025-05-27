package com.dearwith.dearwith_backend.user.domain;

/**
 * 약관 동의 종류
 */
public enum AgreementType {
    AGE_OVER_14,         // [필수] 만 14세 이상
    TERMS_OF_SERVICE,    // [필수] 서비스 이용 약관 동의
    PERSONAL_INFORMATION,// [필수] 개인정보 수집 및 이용 동의
    MARKETING_CONSENT,   // [선택] 마케팅 정보 활용 동의
    PUSH_NOTIFICATION    // [선택] 푸시 알림 수신 동의
}
