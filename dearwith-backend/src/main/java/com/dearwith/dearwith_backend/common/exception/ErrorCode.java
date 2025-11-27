package com.dearwith.dearwith_backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    /* =========================
     * 1. 시스템 / 서버 내부 오류
     * ========================= */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // S3/이미지 관련은 사용자 입장에선 전부 "이미지 처리 중 오류"로 통일
    S3_OBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 이미지를 찾을 수 없습니다."),
    S3_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    S3_COMMIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    IMAGE_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    /* =========================
     * 2. 인증 / 인가 / 계정
     * ========================= */

    // 공통 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "권한이 없습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED,"로그인 정보가 유효하지 않습니다."),
    TOKEN_SUCCESS(HttpStatus.OK, "토큰이 유효합니다."),

    // 로그인 / 계정
    INVALID_EMAIL(HttpStatus.NOT_FOUND, "가입되지 않은 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_SOCIAL_ACCOUNT(HttpStatus.CONFLICT, "이미 연결된 소셜 계정입니다."),

    // 소셜 / 외부 인증
    SOCIAL_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "카카오 로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    INVALID_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),

    // 인증 티켓 (X, 이메일)
    X_TICKET_EXPIRED(HttpStatus.UNAUTHORIZED, "X 인증 티켓이 만료되었거나 유효하지 않습니다."),
    EMAIL_TICKET_EXPIRED_OR_INVALID(HttpStatus.UNAUTHORIZED, "이메일 인증 정보가 만료되었거나 유효하지 않습니다."),
    EMAIL_TICKET_NOT_OWNER(HttpStatus.FORBIDDEN, "이메일 인증 처리 중 오류가 발생했습니다"),
    EMAIL_TICKET_WRONG_PURPOSE(HttpStatus.BAD_REQUEST, "이메일 인증 처리 중 오류가 발생했습니다"),
    EMAIL_TICKET_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "이메일 인증 처리 중 오류가 발생했습니다"),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증 코드가 유효하지 않습니다."),

    /* =========================
     * 3. 약관 / 동의
     * ========================= */
    REQUIRED_AGREEMENT_NOT_CHECKED(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해야 회원가입이 가능합니다."),
    INVALID_AGREEMENT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 약관 타입입니다. 지원하는 타입: AGE_OVER_14, TERMS_OF_SERVICE, PERSONAL_INFORMATION, MARKETING_CONSENT, PUSH_NOTIFICATION"),

    /* =========================
     * 4. 도메인: 이벤트 / 주최자 / 특전
     * ========================= */
    EVENT_START_REQUIRED(HttpStatus.BAD_REQUEST, "이벤트 시작일이 필요합니다."),
    EVENT_DATE_RANGE_INVALID(HttpStatus.BAD_REQUEST, "이벤트 종료일은 시작일 이후여야 합니다."),
    INVALID_EVENT_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 이벤트 상태입니다."),
    ORGANIZER_REQUIRED(HttpStatus.BAD_REQUEST, "주최자 정보는 필수입니다."),
    BENEFIT_DAYINDEX_INVALID(HttpStatus.BAD_REQUEST, "LIMITED 특전의 dayIndex는 1 이상이어야 합니다."),

    /* =========================
     * 5. 도메인: 아티스트 / 그룹 / 북마크 / 좋아요 / 신고
     * ========================= */

    // 아티스트 / 그룹
    ARTIST_REQUIRED(HttpStatus.BAD_REQUEST, "최소 1명의 아티스트가 필요합니다."),
    ARTIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 아티스트입니다."),
    ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 아티스트가 포함되어 있습니다."),
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 그룹입니다."),

    // 북마크 / 좋아요 / 신고
    ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "이미 북마크되었습니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "북마크가 존재하지 않습니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 눌렀습니다."),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "이미 신고하였습니다."),

    /* =========================
     * 6. 파일 / 이미지 / 업로드
     * ========================= */
    INVALID_TMP_KEY(HttpStatus.BAD_REQUEST, "유효하지 않은 tmpKey입니다."),
    DUPLICATE_IMAGE_KEY(HttpStatus.CONFLICT, "중복된 이미지 키가 포함되어 있습니다."),

    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "이미지 용량이 허용 범위를 초과했습니다."),
    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "허용되지 않은 파일 크기입니다."),

    UNSUPPORTED_IMAGE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 이미지 형식입니다."),
    UNSUPPORTED_CONTENT_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 파일 형식입니다."),
    UNSUPPORTED_DOMAIN(HttpStatus.BAD_REQUEST, "허용되지 않은 업로드 도메인입니다."),
    IMAGE_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 이미지입니다."),

    /* =========================
     * 7. 공통 Validation / 요청 오류
     * ========================= */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "잘못된 형식의 입력값입니다."),
    INVALID_PATH_VARIABLE(HttpStatus.BAD_REQUEST, "잘못된 경로 변수입니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /* =========================
     * 8. 공통 Not Found (범용)
     * ========================= */
    NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 데이터입니다.");

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