package com.dearwith.dearwith_backend.common.dto;

import lombok.*;

/**
 * API 에러 응답 공통 객체
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
}
