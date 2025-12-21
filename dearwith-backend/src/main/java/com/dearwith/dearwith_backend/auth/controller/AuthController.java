package com.dearwith.dearwith_backend.auth.controller;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.auth.dto.*;
import com.dearwith.dearwith_backend.auth.enums.ClientPlatform;
import com.dearwith.dearwith_backend.auth.jwt.AuthCookieUtil;
import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.auth.service.EmailVerificationService;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.exception.ErrorResponse;
import com.dearwith.dearwith_backend.user.dto.SignInRequestDto;
import com.dearwith.dearwith_backend.user.dto.SignInResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final EmailVerificationService emailService;
    private final AuthService authService;
    private final AuthCookieUtil authCookieUtil;

    /* ==========================
     * 이메일 인증
     * ========================== */
    @Operation(summary = "이메일 인증 코드 발송",
            description = "purpose : SIGNUP(가입), RESET_PASSWORD(비밀번호 변경)")
    @PostMapping("/signup/email/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendCode(@RequestBody @Valid EmailRequestDto request) {
        emailService.sendVerificationCode(request);
    }

    @Operation(summary = "이메일 인증 코드 검증",
            description = "purpose : SIGNUP(가입), RESET_PASSWORD(비밀번호 변경)")
    @PostMapping("/signup/email/verify")
    public ResponseEntity<EmailTicketResponseDto> verifyCode(@RequestBody @Valid EmailVerifyRequestDto request) {
        return ResponseEntity.ok(emailService.verifyCode(request));
    }

    /* ==========================
     * 이메일 로그인
     * ========================== */

    @Operation(summary = "이메일 로그인(웹) - 쿠키 세팅")
    @PostMapping("/signin")
    public ResponseEntity<SignInResponseDto> signInWeb(
            @RequestBody @Valid SignInRequestDto request,
            HttpServletResponse response
    ) {
        SignInResponseDto full = authService.signInInternal(request, ClientPlatform.WEB);
        setAuthCookies(response, full);
        return ResponseEntity.ok(sanitizeSignIn(full));
    }

    @Operation(summary = "이메일 로그인(앱) - 바디로 FULL 토큰 반환")
    @PostMapping("/signin/native")
    public ResponseEntity<SignInResponseDto> signInNative(
            @RequestBody @Valid SignInRequestDto request
    ) {
        return ResponseEntity.ok(authService.signInInternal(request, ClientPlatform.APP));
    }

    /* ==========================
     * 카카오 로그인
     * ========================== */

    @Operation(summary = "카카오 로그인(웹) - 쿠키 세팅")
    @PostMapping("/oauth/kakao")
    public ResponseEntity<SocialSignInResponseDto> kakaoSignInWeb(
            @RequestBody KakaoSignInRequestDto request,
            HttpServletResponse response
    ) {
        SocialSignInResponseDto full =
                authService.kakaoSignInByCodeInternal(request.getCode(), ClientPlatform.WEB);

        SocialSignInResponseDto body = webSocialLoginResponse(response, full);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "카카오 로그인(앱) - 바디로 FULL 토큰 반환")
    @PostMapping("/oauth/kakao/native")
    public ResponseEntity<SocialSignInResponseDto> kakaoSignInNative(
            @RequestBody KakaoNativeSignInRequestDto request
    ) {
        return ResponseEntity.ok(
                authService.kakaoSignInByAccessTokenInternal(request.getAccessToken(), ClientPlatform.APP)
        );
    }

    /* ==========================
     * 애플 로그인
     * ========================== */

    @Operation(summary = "애플 로그인(웹) - 쿠키 세팅")
    @PostMapping("/oauth/apple")
    public ResponseEntity<SocialSignInResponseDto> appleSignInWeb(
            @RequestBody AppleSignInRequestDto request,
            HttpServletResponse response
    ) {
        SocialSignInResponseDto full = authService.appleSignInInternal(request, ClientPlatform.WEB);

        SocialSignInResponseDto body = webSocialLoginResponse(response, full);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "애플 로그인(앱) - 바디로 FULL 토큰 반환")
    @PostMapping("/oauth/apple/native")
    public ResponseEntity<SocialSignInResponseDto> appleSignInNative(
            @RequestBody AppleSignInRequestDto request
    ) {
        return ResponseEntity.ok(authService.appleSignInInternal(request, ClientPlatform.APP));
    }

    /* ==========================
     * 로그아웃
     * ========================== */

    @Operation(summary = "로그아웃(웹)", description = "쿠키 삭제 + refresh 세션 폐기")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutWeb(
            HttpServletRequest httpRequest,
            @CurrentUser UUID userId,
            HttpServletResponse response
    ) {
        String refreshToken = extractCookie(httpRequest, "REFRESH_TOKEN");
        authService.logoutInternal(userId, null, refreshToken, ClientPlatform.WEB);

        authCookieUtil.clearCookie(response, "ACCESS_TOKEN");
        authCookieUtil.clearCookie(response, "REFRESH_TOKEN");
    }

    @Operation(summary = "로그아웃(앱) - X-Refresh-Token")
    @PostMapping("/logout/native")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutNative(
            HttpServletRequest request,
            @CurrentUser UUID userId,
            @RequestBody(required = false) LogoutRequestDto body
    ) {
        String refreshToken = request.getHeader("X-Refresh-Token");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }
        authService.logoutInternal(userId, body, refreshToken, ClientPlatform.APP);
    }
    /* ==========================
     * 토큰 검증
     * ========================== */

    @Operation(summary = "토큰 유효 검사(웹)")
    @PostMapping("/validate")
    public ResponseEntity<ErrorResponse> validateToken(HttpServletRequest request) {
        String accessToken = extractCookie(request, "ACCESS_TOKEN");
        if (accessToken == null || accessToken.isBlank()) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }
        authService.validateToken(accessToken);
        return ResponseEntity.ok(ErrorResponse.of(ErrorCode.TOKEN_SUCCESS));
    }

    @Operation(summary = "토큰 유효 검사(앱)")
    @PostMapping("/validate/native")
    public ResponseEntity<ErrorResponse> validateTokenNative(HttpServletRequest request) {
        String accessToken = extractBearer(request);
        if (accessToken == null || accessToken.isBlank()) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }
        authService.validateToken(accessToken);
        return ResponseEntity.ok(ErrorResponse.of(ErrorCode.TOKEN_SUCCESS));
    }

    /* ==========================
     * 재발급
     * ========================== */

    @Operation(summary = "토큰 재발급(웹-쿠키) - 쿠키 갱신")
    @PostMapping("/refresh")
    public ResponseEntity<TokenReissueResponseDto> refreshTokenWeb(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractCookie(request, "REFRESH_TOKEN");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        TokenReissueResponseDto full = authService.reissueTokenInternal(refreshToken, ClientPlatform.WEB);

        // rotation 전제: access + refresh 둘 다 갱신
        authCookieUtil.addAuthCookies(response, full.getToken(), full.getRefreshToken());

        // 웹은 바디에서 토큰 숨김
        TokenReissueResponseDto body = TokenReissueResponseDto.builder()
                .message(full.getMessage())
                .expirationTime(full.getExpirationTime())
                .build();

        return ResponseEntity.ok(body);
    }

    @Operation(summary = "토큰 재발급(앱) - X-Refresh-Token")
    @PostMapping("/refresh/native")
    public ResponseEntity<TokenReissueResponseDto> refreshTokenNative(HttpServletRequest request) {

        String refreshToken = request.getHeader("X-Refresh-Token");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        return ResponseEntity.ok(authService.reissueTokenInternal(refreshToken, ClientPlatform.APP));
    }

    /* ==========================
     * 공통 헬퍼
     * ========================== */

    private void setAuthCookies(HttpServletResponse response, SignInResponseDto full) {
        authCookieUtil.addAuthCookies(response, full.getToken(), full.getRefreshToken());
    }

    private SocialSignInResponseDto webSocialLoginResponse(HttpServletResponse response, SocialSignInResponseDto full) {
        // 신규 가입 필요면 쿠키/바디 건드릴 게 없음
        if (full.isNeedSignUp() || full.getSignIn() == null) {
            return full;
        }

        SignInResponseDto signIn = full.getSignIn();
        setAuthCookies(response, signIn);

        return SocialSignInResponseDto.builder()
                .needSignUp(false)
                .provider(full.getProvider())
                .socialId(full.getSocialId())
                .signIn(sanitizeSignIn(signIn))
                .build();
    }

    private SignInResponseDto sanitizeSignIn(SignInResponseDto full) {
        return SignInResponseDto.builder()
                .message(full.getMessage())
                .userId(full.getUserId())
                .nickname(full.getNickname())
                .role(full.getRole())
                .build();
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private String extractBearer(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7).trim();
    }
}