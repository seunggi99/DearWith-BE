package com.dearwith.dearwith_backend.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenReissueResponseDto {
    private String token;
    private String refreshToken;
    private String expirationTime;
    private String message;
}
