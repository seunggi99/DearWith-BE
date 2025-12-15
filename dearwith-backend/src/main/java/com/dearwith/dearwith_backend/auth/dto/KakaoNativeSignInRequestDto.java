package com.dearwith.dearwith_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoNativeSignInRequestDto {
    private String accessToken;
}
