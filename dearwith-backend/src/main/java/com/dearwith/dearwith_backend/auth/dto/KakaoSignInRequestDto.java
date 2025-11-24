package com.dearwith.dearwith_backend.auth.dto;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class KakaoSignInRequestDto {
    private String code;
}
