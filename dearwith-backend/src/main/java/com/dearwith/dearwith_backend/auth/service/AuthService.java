package com.dearwith.dearwith_backend.auth.service;

import com.dearwith.dearwith_backend.auth.JwtTokenProvider;
import com.dearwith.dearwith_backend.auth.dto.*;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.user.domain.SocialAccount;
import com.dearwith.dearwith_backend.user.domain.User;
import com.dearwith.dearwith_backend.user.domain.enums.AuthProvider;
import com.dearwith.dearwith_backend.user.domain.enums.Role;
import com.dearwith.dearwith_backend.user.domain.enums.UserStatus;
import com.dearwith.dearwith_backend.user.dto.SignInRequestDto;
import com.dearwith.dearwith_backend.user.dto.SignInResponseDto;
import com.dearwith.dearwith_backend.user.service.SocialAccountService;
import com.dearwith.dearwith_backend.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    @Autowired
    private final JwtTokenProvider jwtTokenProvider;
    @Autowired
    private final UserService userService;
    @Autowired
    private final SocialAccountService socialAccountService;
    @Autowired
    private final KakaoAuthService kakaoAuthService;

    public SignInResponseDto signIn(SignInRequestDto request){
        // 1. 인증 처리 (예외는 전역 핸들러에서 처리)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. 인증 성공 → DB에서 User 조회
        User user = userService.findByEmail(request.getEmail());

        // 3. 토큰 발급 등 후처리
        TokenCreateRequestDto tokenDTO = toTokenDto(user);

        String token = jwtTokenProvider.generateToken(tokenDTO);
        String refreshToken = jwtTokenProvider.generateRefreshToken(tokenDTO);

        userService.updateLastLoginAt(user);

        // 4. 응답 DTO 리턴
        return SignInResponseDto.builder()
                .message("로그인 성공")
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }


    public SignInResponseDto kakaoSignIn(String code) {
        // 1. Kakao에서 받은 유저 정보 추출
        String accessToken = kakaoAuthService.getAccessToken(code);
        KakaoUserInfoDto kakaoUser = kakaoAuthService.getUserInfo(accessToken);

        // 2. SocialAccount 존재 여부 확인
        Optional<SocialAccount> existing = socialAccountService
                .findByProviderAndSocialId(AuthProvider.KAKAO, String.valueOf(kakaoUser.getId()));

        User user;

        if (existing.isPresent()) {
            // 2-1. 기존 회원이면 user 조회
            user = existing.get().getUser();
        } else {
            // 2-2. 신규 회원이면 가입 처리
            user = User.builder()
              //      .email(kakaoUser.getEmail()) // 없으면 null 가능
                    .role(Role.USER)
                    .userStatus(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userService.save(user);

            // 소셜 계정 연결
            socialAccountService.connectSocialAccount(
                    user,
                    AuthProvider.KAKAO,
                    String.valueOf(kakaoUser.getId())
            );

            // 약관 동의도 필요하면 이 시점에서 처리
        }

        // 3. 로그인 처리: 마지막 로그인 시간 갱신
        userService.updateLastLoginAt(user);

        // 4. JWT 발급
        TokenCreateRequestDto tokenDTO = TokenCreateRequestDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        String token = jwtTokenProvider.generateToken(tokenDTO);
        String refreshToken = jwtTokenProvider.generateRefreshToken(tokenDTO);

        return SignInResponseDto.builder()
                .message("카카오 로그인 성공")
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public TokenReissueResponseDto reissueToken(JwtTokenDto refreshTokenDto) {
        String refreshToken = refreshTokenDto.getToken();

        // 1. refreshToken에서 userId(PK) 추출
        UUID userId = jwtTokenProvider.extractUserId(refreshToken);

        User user = userService.findOne(userId);

        // 2. 토큰 유효성 검증 (토큰 주인과 일치 여부까지)
        if (!jwtTokenProvider.validateToken(refreshToken, user.getId())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 3. 새 AccessToken 발급
        TokenCreateRequestDto tokenDTO = toTokenDto(user);
        String newAccessToken = jwtTokenProvider.generateToken(tokenDTO);

        // 4. 응답 DTO 리턴
        return TokenReissueResponseDto.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .expirationTime("10min")
                .message("재발급 성공")
                .build();
    }


    public void validateToken(JwtTokenDto tokenDto) {
        String token = tokenDto.getToken();

        // 1. 토큰에서 userId(PK) 추출
        UUID userId = jwtTokenProvider.extractUserId(token);

        // 2. 토큰 유효성 검사 (PK 기준)
        if (!jwtTokenProvider.validateToken(token, userId)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 아무런 예외가 없으면 OK(200) 응답
    }

    private TokenCreateRequestDto toTokenDto(User user) {
        return TokenCreateRequestDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
