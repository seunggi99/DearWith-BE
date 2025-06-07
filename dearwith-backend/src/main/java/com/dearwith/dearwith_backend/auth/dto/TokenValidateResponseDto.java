package com.dearwith.dearwith_backend.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenValidateResponseDto {
    private int statusCode;
    private String message;
    private String error;
}
