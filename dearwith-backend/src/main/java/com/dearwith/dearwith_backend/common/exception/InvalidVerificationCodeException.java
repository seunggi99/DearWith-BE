package com.dearwith.dearwith_backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 이메일 인증 코드가 유효하지 않을 때 던져지는 예외
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException() {
        super("인증 코드가 유효하지 않습니다.");
    }

    public InvalidVerificationCodeException(String message) {
        super(message);
    }
}
