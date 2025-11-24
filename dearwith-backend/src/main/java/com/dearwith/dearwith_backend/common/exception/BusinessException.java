package com.dearwith.dearwith_backend.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 사용자에게 직접 노출될 메시지 (override 가능).
     * - 있으면 그대로 ErrorResponse.message로 반환됨
     */
    private final String userMessage;

    /**
     * 프론트/클라이언트에 내려보내도 되는 추가 정보.
     */
    private final String detail;

    /**
     * 서버 로그용 디테일.
     */
    private final String logDetail;


    // 1) 기본 생성자: 기본 메시지 사용
    private BusinessException(ErrorCode errorCode,
                              String userMessage,
                              String detail,
                              String logDetail,
                              Throwable cause) {
        super(userMessage != null ? userMessage : errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.detail = detail;
        this.logDetail = logDetail;
    }

    // 1) 기본
    public static BusinessException of(ErrorCode errorCode) {
        return new BusinessException(errorCode, null, null, null, null);
    }

    // 2) 사용자 메시지만 override
    public static BusinessException withMessage(ErrorCode errorCode, String userMessage) {
        return new BusinessException(errorCode, userMessage, null, null, null);
    }

    // 3) 메시지 + 프론트 detail
    public static BusinessException withMessageAndDetail(ErrorCode errorCode, String userMessage, String detail) {
        return new BusinessException(errorCode, userMessage, detail, null, null);
    }

    // 4) 서버 로그용까지
    public static BusinessException withAll(ErrorCode errorCode, String userMessage, String detail, String logDetail, Throwable cause) {
        return new BusinessException(errorCode, userMessage, detail, logDetail, cause);
    }
}