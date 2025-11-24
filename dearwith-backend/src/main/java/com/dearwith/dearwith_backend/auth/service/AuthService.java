package com.dearwith.dearwith_backend.auth.service;

import com.dearwith.dearwith_backend.auth.JwtTokenProvider;
import com.dearwith.dearwith_backend.auth.dto.*;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.user.entity.SocialAccount;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import com.dearwith.dearwith_backend.user.enums.Role;
import com.dearwith.dearwith_backend.user.enums.UserStatus;
import com.dearwith.dearwith_backend.user.dto.SignInRequestDto;
import com.dearwith.dearwith_backend.user.dto.SignInResponseDto;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import com.dearwith.dearwith_backend.user.service.SocialAccountService;
import com.dearwith.dearwith_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
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

    public SignInResponseDto signIn(SignInRequestDto request){
        // 1. 인증 처리
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


    public KakaoSignInResponseDto kakaoSignIn(String code) {
        AuthProvider provider = AuthProvider.KAKAO;

        String accessToken;
        KakaoUserInfoDto kakaoUser;

        try {
            // 1) 토큰 요청
            accessToken = kakaoAuthService.getAccessToken(code);

            if (accessToken == null || accessToken.isBlank()) {
                throw new BusinessException(ErrorCode.SOCIAL_AUTH_FAILED, "카카오 인증 토큰이 비어 있음");
            }

            // 2) 사용자 정보 요청
            kakaoUser = kakaoAuthService.getUserInfo(accessToken);

            if (kakaoUser == null) {
                throw new BusinessException(ErrorCode.SOCIAL_AUTH_FAILED, "카카오 사용자 정보 조회 실패");
            }

        } catch (HttpClientErrorException e) {
            // Kakao가 400, 401 등 내려준 경우
            throw new BusinessException(ErrorCode.SOCIAL_AUTH_FAILED,
                    "카카오 인증 실패: " + e.getStatusCode() + " / " + e.getMessage());
        } catch (Exception e) {
            // 기타 오류
            throw new BusinessException(ErrorCode.SOCIAL_AUTH_FAILED,
                    "카카오 로그인 처리 중 오류 발생: " + e.getMessage());
        }

        String socialId = String.valueOf(kakaoUser.getId());

        Optional<SocialAccount> existing =
                socialAccountService.findByProviderAndSocialId(provider, socialId);

        if (existing.isPresent()) {
            User user = existing.get().getUser();

            userService.updateLastLoginAt(user);

            TokenCreateRequestDto tokenDTO = TokenCreateRequestDto.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .build();

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

        // 신규 회원
        return KakaoSignInResponseDto.builder()
                .needSignUp(true)
                .signIn(null)
                .provider(provider)
                .socialId(socialId)
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

    public void validateOwner(User owner, UUID userId, String message) {

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));

        if (requester.isAdmin()) {
            return;
        }

        if (owner == null || owner.getId() == null || !owner.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, message);
        }
    }
}
