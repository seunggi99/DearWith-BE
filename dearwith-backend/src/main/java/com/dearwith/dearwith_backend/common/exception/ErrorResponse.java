package com.dearwith.dearwith_backend.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;


@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String detail;
    private final String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final OffsetDateTime timestamp;

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.name(),
                errorCode.getMessage(),
                null,
                null,
                OffsetDateTime.now()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String detail, String path) {
        return new ErrorResponse(
                errorCode.name(),
                errorCode.getMessage(),
                detail,
                path,
                OffsetDateTime.now()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String detail, String path) {
        return new ErrorResponse(
                errorCode.name(),
                message != null ? message : errorCode.getMessage(),
                detail,
                path,
                OffsetDateTime.now()
        );
    }
}