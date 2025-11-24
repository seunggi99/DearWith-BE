package com.dearwith.dearwith_backend.auth.service;

import com.dearwith.dearwith_backend.auth.JwtTokenProvider;
import com.dearwith.dearwith_backend.auth.dto.*;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.user.entity.SocialAccount;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import com.dearwith.dearwith_backend.user.dto.SignInRequestDto;
import com.dearwith.dearwith_backend.user.dto.SignInResponseDto;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import com.dearwith.dearwith_backend.user.service.SocialAccountService;
import com.dearwith.dearwith_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final SocialAccountService socialAccountService;
    private final KakaoAuthService kakaoAuthService;
    private final UserRepository userRepository;


    /*──────────────────────────────────────────────
     | 1. 이메일 로그인
     *──────────────────────────────────────────────*/
    public SignInResponseDto signIn(SignInRequestDto request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            throw e;
        }

        User user = userService.findByEmail(request.getEmail());
        TokenCreateRequestDto tokenDTO = toTokenDto(user);

        String token = jwtTokenProvider.generateToken(tokenDTO);
        String refreshToken = jwtTokenProvider.generateRefreshToken(tokenDTO);

        userService.updateLastLoginAt(user);

        return SignInResponseDto.builder()
                .message("로그인 성공")
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }


    /*──────────────────────────────────────────────
     | 2. 카카오 로그인
     *──────────────────────────────────────────────*/
    public KakaoSignInResponseDto kakaoSignIn(String code) {

        AuthProvider provider = AuthProvider.KAKAO;
        String accessToken;
        KakaoUserInfoDto kakaoUser;

        try {
            accessToken = kakaoAuthService.getAccessToken(code);

            if (accessToken == null || accessToken.isBlank()) {
                throw BusinessException.withMessage(
                        ErrorCode.KAKAO_AUTH_FAILED,
                        "카카오 인증에 실패했습니다. 잠시 후 다시 시도해주세요."
                );
            }

            kakaoUser = kakaoAuthService.getUserInfo(accessToken);

            if (kakaoUser == null) {
                throw BusinessException.withMessage(
                        ErrorCode.KAKAO_AUTH_FAILED,
                        "카카오 사용자 정보를 가져오지 못했습니다."
                );
            }

        } catch (HttpClientErrorException e) {
            throw BusinessException.withAll(
                    ErrorCode.KAKAO_AUTH_FAILED,
                    null,
                    "KAKAO_HTTP_ERROR",
                    "Kakao API error: status=" + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            throw BusinessException.withAll(
                    ErrorCode.KAKAO_AUTH_FAILED,
                    null,
                    "KAKAO_UNKNOWN_ERROR",
                    "Unknown Kakao login error: " + e.getMessage(),
                    e
            );
        }

        String socialId = String.valueOf(kakaoUser.getId());

        Optional<SocialAccount> existing =
                socialAccountService.findByProviderAndSocialId(provider, socialId);

        /* 기존 계정 있음 → 바로 로그인 처리 */
        if (existing.isPresent()) {
            User user = existing.get().getUser();
            userService.updateLastLoginAt(user);

            TokenCreateRequestDto tokenDTO = toTokenDto(user);

            String token = jwtTokenProvider.generateToken(tokenDTO);
            String refreshToken = jwtTokenProvider.generateRefreshToken(tokenDTO);

            SignInResponseDto signIn = SignInResponseDto.builder()
                    .message("카카오 로그인 성공")
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .role(user.getRole())
                    .token(token)
                    .refreshToken(refreshToken)
                    .build();

            return KakaoSignInResponseDto.builder()
                    .needSignUp(false)
                    .signIn(signIn)
                    .provider(provider)
                    .socialId(socialId)
                    .build();
        }

        /* 신규 회원 */
        return KakaoSignInResponseDto.builder()
                .needSignUp(true)
                .provider(provider)
                .socialId(socialId)
                .signIn(null)
                .build();
    }


    /*──────────────────────────────────────────────
     | 3. 토큰 재발급
     *──────────────────────────────────────────────*/
    public TokenReissueResponseDto reissueToken(JwtTokenDto refreshTokenDto) {
        String refreshToken = refreshTokenDto.getToken();

        UUID userId;
        try {
            userId = jwtTokenProvider.extractUserId(refreshToken);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);

        }

        User user = userService.findOne(userId);

        if (!jwtTokenProvider.validateToken(refreshToken, user.getId())) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);

        }

        TokenCreateRequestDto tokenDTO = toTokenDto(user);
        String newAccessToken = jwtTokenProvider.generateToken(tokenDTO);

        return TokenReissueResponseDto.builder()
                .message("토큰 재발급 성공")
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .expirationTime("10min")
                .build();
    }


    /*──────────────────────────────────────────────
     | 4. 토큰 유효성 검사
     *──────────────────────────────────────────────*/
    public void validateToken(JwtTokenDto tokenDto) {
        String token = tokenDto.getToken();

        UUID userId;
        try {
            userId = jwtTokenProvider.extractUserId(token);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);

        }

        if (!jwtTokenProvider.validateToken(token, userId)) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }
    }


    /*──────────────────────────────────────────────
     | 5. Helper
     *──────────────────────────────────────────────*/
    private TokenCreateRequestDto toTokenDto(User user) {
        return TokenCreateRequestDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }


    /*──────────────────────────────────────────────
     | 6. Owner 검증
     *──────────────────────────────────────────────*/
    public void validateOwner(User owner, UUID userId, String message) {

        User requester = userRepository.findById(userId)
                .orElseThrow(() ->
                        BusinessException.withMessage(
                                ErrorCode.NOT_FOUND,
                                "존재하지 않는 사용자입니다."
                        )
                );

        if (requester.isAdmin()) {
            return;
        }

        if (owner == null || owner.getId() == null || !owner.getId().equals(userId)) {
            throw BusinessException.withMessage(
                    ErrorCode.UNAUTHORIZED,
                    message != null ? message : "권한이 없습니다."
            );
        }
    }
}