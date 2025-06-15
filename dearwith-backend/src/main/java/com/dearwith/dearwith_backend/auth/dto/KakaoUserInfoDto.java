package com.dearwith.dearwith_backend.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KakaoUserInfoDto {
    private Long id;
    private String email;
    private String nickname;
}
