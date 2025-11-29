package com.dearwith.dearwith_backend.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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


    /** ⭐ 추가: detail 에 Map을 넣고 싶을 때 (JSON으로 변환) */
    public static BusinessException withDetailMap(
            ErrorCode errorCode,
            String userMessage,
            Map<String, Object> detailMap
    ) {
        String jsonDetail = convertToJson(detailMap);
        return new BusinessException(
                errorCode,
                userMessage,
                jsonDetail,  // 프론트 개발용 detail(JSON)
                null,
                null
        );
    }


    private static String convertToJson(Map<String, Object> map) {
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            // JSON 변환 실패 시, 그래도 안전하게 문자열 형태로 바로 넣어준다.
            return map.toString();
        }
    }
}