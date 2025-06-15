package com.dearwith.dearwith_backend.common.exception;

import lombok.*;
import org.springframework.http.HttpStatus;

public enum ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 데이터입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "권한이 없습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."),
    TOKEN_SUCCESS(HttpStatus.OK, "토큰이 유효합니다."),
    OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리에 실패했습니다."),
    INVALID_EMAIL(HttpStatus.NOT_FOUND, "가입되지 않은 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증 코드가 유효하지 않습니다."),
    REQUIRED_AGREEMENT_NOT_CHECKED(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해야 회원가입이 가능합니다."),
    INVALID_AGREEMENT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 약관 타입입니다. 지원하는 타입: AGE_OVER_14, TERMS_OF_SERVICE, PERSONAL_INFORMATION, MARKETING_CONSENT, PUSH_NOTIFICATION"),
    DUPLICATE_SOCIAL_ACCOUNT(HttpStatus.CONFLICT, "이미 연결된 소셜 계정입니다."),
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    public String getMessage() {
        return message;
    }
}

