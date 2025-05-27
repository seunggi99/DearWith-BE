package com.dearwith.dearwith_backend.auth.service;

public interface EmailVerificationService {
    /** 이메일로 인증 코드를 생성해서 발송(10분 TTL) */
    void sendVerificationCode(String email);

    /** 이메일 + 코드 검증, 맞으면 즉시 만료 처리 */
    void verifyCode(String email, String code);
}