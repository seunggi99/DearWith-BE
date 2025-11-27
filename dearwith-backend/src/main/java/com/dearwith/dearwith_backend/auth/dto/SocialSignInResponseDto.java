package com.dearwith.dearwith_backend.auth.dto;

import com.dearwith.dearwith_backend.user.dto.SignInResponseDto;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialSignInResponseDto {

    /**
     * true  = 우리 서비스에 아직 회원이 아님 → 추가 정보 입력 + 약관 동의 필요
     * false = 기존 회원 → signIn에 로그인 결과가 들어있음
     */
    private boolean needSignUp;

    /**
     * 기존 회원일 때만 채워지는 로그인 결과 (JWT, userId, nickname 등)
     */
    private SignInResponseDto signIn;

    private AuthProvider provider;          // KAKAO
    private String socialId;                // 신규가입일 때만 사용
}
