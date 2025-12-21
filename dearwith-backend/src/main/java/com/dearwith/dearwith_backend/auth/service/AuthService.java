package com.dearwith.dearwith_backend.auth.service;

import com.dearwith.dearwith_backend.auth.dto.*;
import com.dearwith.dearwith_backend.auth.enums.ClientPlatform;
import com.dearwith.dearwith_backend.auth.jwt.JwtTokenProvider;
import com.dearwith.dearwith_backend.auth.jwt.RefreshTokenStore;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.log.BusinessLogService;
import com.dearwith.dearwith_backend.external.apple.AppleIdTokenClaims;
import com.dearwith.dearwith_backend.external.apple.AppleIdTokenVerifier;
import com.dearwith.dearwith_backend.external.apple.AppleTokenClient;
import com.dearwith.dearwith_backend.external.apple.AppleTokenResponse;
import com.dearwith.dearwith_backend.logging.constant.BusinessAction;
import com.dearwith.dearwith_backend.logging.constant.TargetType;
import com.dearwith.dearwith_backend.logging.enums.BusinessLogCategory;
import com.dearwith.dearwith_backend.notification.service.PushDeviceService;
import com.dearwith.dearwith_backend.user.dto.SignInRequestDto;
import com.dearwith.dearwith_backend.user.dto.SignInResponseDto;
import com.dearwith.dearwith_backend.user.entity.SocialAccount;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import com.dearwith.dearwith_backend.user.service.SocialAccountService;
import com.dearwith.dearwith_backend.user.service.UserReader;
import com.dearwith.dearwith_backend.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final SocialAccountService socialAccountService;
    private final KakaoAuthService kakaoAuthService;
    private final AppleTokenClient appleTokenClient;
    private final AppleIdTokenVerifier appleIdTokenVerifier;
    private final UserReader userReader;
    private final PushDeviceService pushDeviceService;
    private final BusinessLogService businessLogService;
    private final RefreshTokenStore refreshTokenStore;

    /* ==========================
     * 공통: 토큰 발급 + refresh 저장
     * ========================== */
    private SignInResponseDto issueTokens(User user, String message, ClientPlatform platform) {
        TokenCreateRequestDto tokenDTO = toTokenDto(user);

        String accessToken = jwtTokenProvider.generateAccessToken(tokenDTO);
        String refreshToken = (platform == ClientPlatform.WEB)
                ? jwtTokenProvider.generateRefreshTokenWeb(tokenDTO)
                : jwtTokenProvider.generateRefreshTokenApp(tokenDTO);

        storeRefresh(refreshToken, user.getId());

        return SignInResponseDto.builder()
                .message(message)
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void storeRefresh(String refreshToken, UUID userId) {
        final Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(refreshToken);
        } catch (ExpiredJwtException e) {
            throw BusinessException.of(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        String typ = claims.get("typ", String.class);
        if (!JwtTokenProvider.TYP_REFRESH.equals(typ)) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        String jti = claims.getId();
        Date exp = claims.getExpiration();

        if (jti == null || jti.isBlank() || exp == null) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        long ttlMs = exp.getTime() - System.currentTimeMillis();
        if (ttlMs <= 0) throw BusinessException.of(ErrorCode.TOKEN_INVALID);

        refreshTokenStore.save(jti, userId.toString(), java.time.Duration.ofMillis(ttlMs));
    }

    /* ==========================
     * 1) 이메일 로그인
     * ========================== */
    @Transactional
    public SignInResponseDto signInInternal(SignInRequestDto request, ClientPlatform platform) {
        User user = userService.findByEmail(request.getEmail());
        userReader.getLoginAllowedUser(user.getId());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            businessLogService.warn(
                    BusinessLogCategory.AUTH,
                    BusinessAction.Auth.EMAIL_SIGN_IN_FAILED,
                    null,
                    TargetType.USER,
                    null,
                    "이메일 로그인 실패",
                    logDetails(platform, Map.of("email", request.getEmail()))
            );
            throw e;
        }

        userService.updateLastLoginAt(user);

        businessLogService.info(
                BusinessLogCategory.AUTH,
                BusinessAction.Auth.EMAIL_SIGN_IN_SUCCESS,
                user.getId(),
                TargetType.USER,
                null,
                "이메일 로그인 성공",
                logDetails(platform, Map.of("email", user.getEmail()))
        );

        return issueTokens(user, "로그인 성공", platform);
    }

    /* ==========================
     * 2) 카카오 로그인
     * ========================== */
    @Transactional
    public SocialSignInResponseDto kakaoSignInByCodeInternal(String code, ClientPlatform platform) {
        AuthProvider provider = AuthProvider.KAKAO;

        String accessToken = kakaoAuthService.getAccessToken(code);
        if (accessToken == null || accessToken.isBlank()) {
            throw BusinessException.withMessage(ErrorCode.KAKAO_AUTH_FAILED,
                    "카카오 인증에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        KakaoUserInfoDto kakaoUser = kakaoAuthService.getUserInfo(accessToken);
        if (kakaoUser == null) {
            throw BusinessException.withMessage(ErrorCode.KAKAO_AUTH_FAILED,
                    "카카오 사용자 정보를 가져오지 못했습니다.");
        }

        return handleKakaoUser(provider, kakaoUser, platform);
    }

    @Transactional
    public SocialSignInResponseDto kakaoSignInByAccessTokenInternal(String accessToken, ClientPlatform platform) {
        AuthProvider provider = AuthProvider.KAKAO;

        if (accessToken == null || accessToken.isBlank()) {
            throw BusinessException.withMessage(ErrorCode.KAKAO_AUTH_FAILED,
                    "카카오 인증에 실패했습니다. 다시 시도해주세요.");
        }

        KakaoUserInfoDto kakaoUser = kakaoAuthService.getUserInfo(accessToken);
        if (kakaoUser == null) {
            throw BusinessException.withMessage(ErrorCode.KAKAO_AUTH_FAILED,
                    "카카오 사용자 정보를 가져오지 못했습니다.");
        }

        return handleKakaoUser(provider, kakaoUser, platform);
    }

    private SocialSignInResponseDto handleKakaoUser(AuthProvider provider, KakaoUserInfoDto kakaoUser, ClientPlatform platform) {
        String socialId = String.valueOf(kakaoUser.getId());

        Optional<SocialAccount> existing =
                socialAccountService.findByProviderAndSocialId(provider, socialId);

        if (existing.isPresent()) {
            User user = existing.get().getUser();

            if (user.getDeletedAt() != null) {
                businessLogService.info(
                        BusinessLogCategory.AUTH,
                        BusinessAction.Auth.KAKAO_NEED_SIGNUP,
                        null,
                        TargetType.USER,
                        null,
                        "카카오 탈퇴 회원 재가입 필요",
                        logDetails(platform, Map.of("socialId", socialId))
                );

                return SocialSignInResponseDto.builder()
                        .needSignUp(true)
                        .provider(provider)
                        .socialId(socialId)
                        .signIn(null)
                        .build();
            }

            userReader.getLoginAllowedUser(user.getId());
            userService.updateLastLoginAt(user);

            SignInResponseDto signIn = issueTokens(user, "카카오 로그인 성공", platform);

            businessLogService.info(
                    BusinessLogCategory.AUTH,
                    BusinessAction.Auth.KAKAO_SIGN_IN_SUCCESS,
                    user.getId(),
                    TargetType.USER,
                    null,
                    "카카오 로그인 성공",
                    logDetails(platform, Map.of("socialId", socialId))
            );

            return SocialSignInResponseDto.builder()
                    .needSignUp(false)
                    .provider(provider)
                    .socialId(socialId)
                    .signIn(signIn)
                    .build();
        }

        businessLogService.info(
                BusinessLogCategory.AUTH,
                BusinessAction.Auth.KAKAO_NEED_SIGNUP,
                null,
                TargetType.USER,
                null,
                "카카오 신규 회원 가입 필요",
                logDetails(platform, Map.of("socialId", socialId))
        );

        return SocialSignInResponseDto.builder()
                .needSignUp(true)
                .provider(provider)
                .socialId(socialId)
                .signIn(null)
                .build();
    }

    /* ==========================
     * 3) 애플 로그인
     * ========================== */
    @Transactional
    public SocialSignInResponseDto appleSignInInternal(AppleSignInRequestDto request, ClientPlatform platform) {
        AuthProvider provider = AuthProvider.APPLE;

        if ((request.authorizationCode() == null || request.authorizationCode().isBlank())
                && (request.idToken() == null || request.idToken().isBlank())) {
            throw BusinessException.withMessage(ErrorCode.OPERATION_FAILED, "애플 로그인 정보가 올바르지 않습니다.");
        }

        String idToken = request.idToken();

        try {
            if (idToken == null || idToken.isBlank()) {
                AppleTokenResponse tokenResponse =
                        appleTokenClient.exchangeCodeForToken(request.authorizationCode());
                idToken = tokenResponse.idToken();
                if (idToken == null || idToken.isBlank()) {
                    throw BusinessException.withMessage(ErrorCode.OPERATION_FAILED, "애플 토큰 응답에 ID 토큰이 없습니다.");
                }
            }

            AppleIdTokenClaims claims = appleIdTokenVerifier.verify(idToken);
            String socialId = claims.sub();
            if (socialId == null || socialId.isBlank()) {
                throw BusinessException.withMessage(ErrorCode.OPERATION_FAILED, "애플 계정 정보에 사용자 식별자가 없습니다.");
            }

            Optional<SocialAccount> existing =
                    socialAccountService.findByProviderAndSocialId(provider, socialId);

            if (existing.isPresent()) {
                User user = existing.get().getUser();

                if (user.getDeletedAt() != null) {
                    businessLogService.info(
                            BusinessLogCategory.AUTH,
                            BusinessAction.Auth.APPLE_NEED_SIGNUP,
                            null,
                            TargetType.USER,
                            null,
                            "애플 탈퇴 회원 재가입 필요",
                            logDetails(platform, Map.of("socialId", socialId))
                    );

                    return SocialSignInResponseDto.builder()
                            .needSignUp(true)
                            .provider(provider)
                            .socialId(socialId)
                            .signIn(null)
                            .build();
                }

                userReader.getLoginAllowedUser(user.getId());
                userService.updateLastLoginAt(user);

                SignInResponseDto signIn = issueTokens(user, "애플 로그인 성공", platform);

                businessLogService.info(
                        BusinessLogCategory.AUTH,
                        BusinessAction.Auth.APPLE_SIGN_IN_SUCCESS,
                        user.getId(),
                        TargetType.USER,
                        null,
                        "애플 로그인 성공",
                        logDetails(platform, Map.of("socialId", socialId))
                );

                return SocialSignInResponseDto.builder()
                        .needSignUp(false)
                        .provider(provider)
                        .socialId(socialId)
                        .signIn(signIn)
                        .build();
            }

            businessLogService.info(
                    BusinessLogCategory.AUTH,
                    BusinessAction.Auth.APPLE_NEED_SIGNUP,
                    null,
                    TargetType.USER,
                    null,
                    "애플 신규 회원 가입 필요",
                    logDetails(platform, Map.of("socialId", socialId))
            );

            return SocialSignInResponseDto.builder()
                    .needSignUp(true)
                    .provider(provider)
                    .socialId(socialId)
                    .signIn(null)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            businessLogService.error(
                    BusinessLogCategory.AUTH,
                    BusinessAction.Auth.APPLE_SIGN_IN_FAILED,
                    null,
                    TargetType.USER,
                    null,
                    "애플 로그인 처리 중 오류",
                    logDetails(platform, Map.of("errorMessage", e.getMessage())),
                    e
            );
            throw BusinessException.withAll(
                    ErrorCode.OPERATION_FAILED,
                    null,
                    "APPLE_LOGIN_ERROR",
                    "애플 로그인 처리 중 오류: " + e.getMessage(),
                    e
            );
        }
    }

    /* ==========================
     * 4) 재발급
     * ========================== */
    @Transactional
    public TokenReissueResponseDto reissueTokenInternal(String refreshToken, ClientPlatform platform) {

        final Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(refreshToken);
        } catch (ExpiredJwtException e) {

            businessLogService.warn(
                    BusinessLogCategory.AUTH,
                    BusinessAction.Auth.TOKEN_REISSUE_FAILED,
                    null,
                    TargetType.USER,
                    null,
                    "토큰 재발급 실패: refresh 만료",
                    logDetails(platform)
            );

            throw BusinessException.of(ErrorCode.REFRESH_TOKEN_EXPIRED);

        } catch (JwtException | IllegalArgumentException e) {

            businessLogService.warn(
                    BusinessLogCategory.AUTH,
                    BusinessAction.Auth.TOKEN_REISSUE_FAILED,
                    null,
                    TargetType.USER,
                    null,
                    "토큰 재발급 실패: refresh invalid",
                    logDetails(platform)
            );

            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        String typ = claims.get("typ", String.class);
        String oldJti = claims.getId();

        if (!JwtTokenProvider.TYP_REFRESH.equals(typ)
                || oldJti == null
                || !refreshTokenStore.exists(oldJti)) {

            businessLogService.warn(
                    BusinessLogCategory.AUTH,
                    BusinessAction.Auth.TOKEN_REISSUE_FAILED,
                    null,
                    TargetType.USER,
                    null,
                    "토큰 재발급 실패: refresh invalid/rotation",
                    logDetails(platform)
            );

            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        UUID userId;
        try {
            userId = UUID.fromString(claims.get("userId", String.class));
        } catch (Exception e) {

            businessLogService.warn(
                    BusinessLogCategory.AUTH,
                    BusinessAction.Auth.TOKEN_REISSUE_FAILED,
                    null,
                    TargetType.USER,
                    null,
                    "토큰 재발급 실패: userId invalid",
                    logDetails(platform)
            );

            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        User user = userReader.getLoginAllowedUser(userId);

        refreshTokenStore.delete(oldJti);

        TokenCreateRequestDto tokenDTO = toTokenDto(user);
        String newAccess = jwtTokenProvider.generateAccessToken(tokenDTO);
        String newRefresh = (platform == ClientPlatform.WEB)
                ? jwtTokenProvider.generateRefreshTokenWeb(tokenDTO)
                : jwtTokenProvider.generateRefreshTokenApp(tokenDTO);

        storeRefresh(newRefresh, userId);

        businessLogService.info(
                BusinessLogCategory.AUTH,
                (platform == ClientPlatform.WEB)
                        ? BusinessAction.Auth.TOKEN_REISSUE_SUCCESS_WEB
                        : BusinessAction.Auth.TOKEN_REISSUE_SUCCESS_APP,
                userId,
                TargetType.USER,
                null,
                "토큰 재발급 성공",
                Map.of(
                        "platform", String.valueOf(platform),
                        "userId", String.valueOf(userId),
                        "oldJti", oldJti,
                        "newRefreshJti", jwtTokenProvider.parseClaims(newRefresh).getId()
                )
        );

        return TokenReissueResponseDto.builder()
                .message("토큰 재발급 성공")
                .token(newAccess)
                .refreshToken(newRefresh)
                .expirationTime("10min")
                .build();
    }

    /* ==========================
     * 5) 로그아웃 (refresh revoke)
     * ========================== */
    @Transactional
    public void logoutInternal(UUID userId, LogoutRequestDto request, String refreshToken, ClientPlatform platform) {

        if (request != null) {
            pushDeviceService.unregister(userId, request.deviceId(), request.fcmToken());
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                String typ = jwtTokenProvider.extractClaim(refreshToken, c -> (String) c.get("typ"));
                if (JwtTokenProvider.TYP_REFRESH.equals(typ)) {
                    String jti = jwtTokenProvider.extractJti(refreshToken);
                    if (jti != null && !jti.isBlank()) {
                        refreshTokenStore.delete(jti);
                    }
                }
            } catch (Exception e) {
                log.debug("logout refresh revoke skipped: {}", e.getMessage());
            }
        }

        Map<String, Object> details = logDetails(platform);
        details.put("deviceId", request != null ? request.deviceId() : null);
        details.put("hasFcmToken", request != null && request.fcmToken() != null);

        businessLogService.info(
                BusinessLogCategory.AUTH,
                BusinessAction.Auth.LOGOUT,
                userId,
                TargetType.USER,
                null,
                "로그아웃",
                details
        );
    }

    /* ==========================
     * 토큰 검증
     * ========================== */

    @Transactional(readOnly = true)
    public void validateToken(String token) {
        validateToken(token, null);
    }

    @Transactional(readOnly = true)
    public void validateToken(String token, ClientPlatform platform) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token);
            String typ = claims.get("typ", String.class);
            if (!JwtTokenProvider.TYP_ACCESS.equals(typ)) {
                throw BusinessException.of(ErrorCode.TOKEN_INVALID);
            }
        } catch (ExpiredJwtException e) {
            throw BusinessException.of(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }
    }

    private TokenCreateRequestDto toTokenDto(User user) {
        return TokenCreateRequestDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private Map<String, Object> logDetails(ClientPlatform platform) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("clientPlatform", platform != null ? platform.name() : null);
        return m;
    }

    private Map<String, Object> logDetails(ClientPlatform platform, Map<String, Object> extra) {
        Map<String, Object> m = logDetails(platform);
        if (extra != null) m.putAll(extra);
        return m;
    }

    /* ──────────────────────────────────────────────
     * Owner 검증
     * ────────────────────────────────────────────── */

    public void validateOwner(User owner, User requester, String message) {
        validateOwner(owner, requester, message, null);
    }

    public void validateOwner(User owner, User requester, String message, ClientPlatform platform) {

        if (requester == null) {

            businessLogService.warn(
                    BusinessLogCategory.AUTH,
                    BusinessAction.Auth.OWNER_VALIDATE_REQUESTER_NOT_FOUND,
                    null,
                    TargetType.USER,
                    null,
                    "소유자 검증 실패: requester 없음",
                    logDetails(platform, Map.of("message", message))
            );

            throw BusinessException.withMessage(
                    ErrorCode.NOT_FOUND,
                    "존재하지 않는 사용자입니다."
            );
        }

        if (requester.isAdmin()) return;

        if (owner == null || owner.getId() == null || !owner.getId().equals(requester.getId())) {

            Map<String, Object> details = logDetails(platform);
            details.put("message", message);
            details.put("requesterId", requester.getId().toString());

            if (owner != null && owner.getId() != null) {
                details.put("ownerId", owner.getId().toString());
            }

            businessLogService.warn(
                    BusinessLogCategory.AUTH,
                    BusinessAction.Auth.OWNER_VALIDATE_UNAUTHORIZED,
                    requester.getId(),
                    TargetType.USER,
                    null,
                    "소유자 검증 실패: 권한 없음",
                    details
            );

            throw BusinessException.withMessage(
                    ErrorCode.UNAUTHORIZED,
                    "권한이 없습니다."
            );
        }
    }
}