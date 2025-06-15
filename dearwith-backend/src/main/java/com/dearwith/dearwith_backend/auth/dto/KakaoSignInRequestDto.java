package com.dearwith.dearwith_backend.auth.dto;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class KakaoSignInRequestDto {
    private String code; // 프론트에서 받은 인가코드
}
