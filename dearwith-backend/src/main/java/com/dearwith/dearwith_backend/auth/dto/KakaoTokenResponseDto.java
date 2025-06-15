package com.dearwith.dearwith_backend.auth.dto;

import lombok.Data;

@Data
public class KakaoTokenResponseDto {
    private String access_token;
    private String refresh_token;
    private String id_token;
    private String token_type;
    private int expires_in;
}
