package com.dearwith.dearwith_backend.auth.controller;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.auth.jwt.AuthCookieUtil;
import com.dearwith.dearwith_backend.auth.dto.*;
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

    @Operation(summary = "로그인")
    @PostMapping("/signin")
    public ResponseEntity<SignInResponseDto> signIn(
            @RequestBody @Valid SignInRequestDto request,
            HttpServletResponse response
    ){
        SignInResponseDto signIn = authService.signIn(request);

        // 1) 쿠키 세팅
        authCookieUtil.addAuthCookies(
                response,
                signIn.getToken(),
                signIn.getRefreshToken()
        );

        // 2) 응답 바디에서는 토큰 제거
        SignInResponseDto body = SignInResponseDto.builder()
                .message(signIn.getMessage())
                .userId(signIn.getUserId())
                .nickname(signIn.getNickname())
                .role(signIn.getRole())
                .build();

        return ResponseEntity.ok(body);
    }

    @Operation(summary = "카카오 로그인(웹)")
    @PostMapping("/oauth/kakao")
    public ResponseEntity<SocialSignInResponseDto> kakaoSignIn(
            @RequestBody KakaoSignInRequestDto request,
            HttpServletResponse response
    ) {
        SocialSignInResponseDto res = authService.kakaoSignIn(request.getCode());
        // 기존 계정 로그인인 경우에만 쿠키 세팅
        if (!res.isNeedSignUp() && res.getSignIn() != null) {
            SignInResponseDto signIn = res.getSignIn();

            authCookieUtil.addAuthCookies(
                    response,
                    signIn.getToken(),
                    signIn.getRefreshToken()
            );

            SignInResponseDto sanitized = SignInResponseDto.builder()
                    .message(signIn.getMessage())
                    .userId(signIn.getUserId())
                    .nickname(signIn.getNickname())
                    .role(signIn.getRole())
                    .build();

            res = SocialSignInResponseDto.builder()
                    .needSignUp(false)
                    .provider(res.getProvider())
                    .socialId(res.getSocialId())
                    .signIn(sanitized)
                    .build();
        }
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "카카오 로그인(앱)")
    @PostMapping("/oauth/kakao/native")
    public ResponseEntity<SocialSignInResponseDto> NativeKakaoSignIn(
            @RequestBody KakaoNativeSignInRequestDto request,
            HttpServletResponse response
    ) {
        SocialSignInResponseDto res = authService.kakaoSignInWithAccessToken(request.getAccessToken());

        if (!res.isNeedSignUp() && res.getSignIn() != null) {
            SignInResponseDto signIn = res.getSignIn();

            authCookieUtil.addAuthCookies(
                    response,
                    signIn.getToken(),
                    signIn.getRefreshToken()
            );

            SignInResponseDto sanitized = SignInResponseDto.builder()
                    .message(signIn.getMessage())
                    .userId(signIn.getUserId())
                    .nickname(signIn.getNickname())
                    .role(signIn.getRole())
                    .build();

            res = SocialSignInResponseDto.builder()
                    .needSignUp(false)
                    .provider(res.getProvider())
                    .socialId(res.getSocialId())
                    .signIn(sanitized)
                    .build();
        }
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "애플 로그인")
    @PostMapping("/oauth/apple")
    public ResponseEntity<SocialSignInResponseDto> appleSignIn(
            @RequestBody AppleSignInRequestDto request,
            HttpServletResponse response
    ) {
        SocialSignInResponseDto res = authService.appleSignIn(request);

        if (!res.isNeedSignUp() && res.getSignIn() != null) {
            SignInResponseDto signIn = res.getSignIn();

            authCookieUtil.addAuthCookies(
                    response,
                    signIn.getToken(),
                    signIn.getRefreshToken()
            );

            SignInResponseDto sanitized = SignInResponseDto.builder()
                    .message(signIn.getMessage())
                    .userId(signIn.getUserId())
                    .nickname(signIn.getNickname())
                    .role(signIn.getRole())
                    .build();

            res = SocialSignInResponseDto.builder()
                    .needSignUp(false)
                    .provider(res.getProvider())
                    .socialId(res.getSocialId())
                    .signIn(sanitized)
                    .build();
        }

        return ResponseEntity.ok(res);
    }

    @Operation(summary = "로그아웃", description = "쿠키에서 토큰 삭제, deviceId,fcmToken 로 기기등록 해제(웹에선 null) ")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CurrentUser UUID userId,
            @RequestBody(required = false) LogoutRequestDto request,
            HttpServletResponse response
    ) {
        authService.logout(userId, request);
        authCookieUtil.clearCookie(response, "ACCESS_TOKEN");
        authCookieUtil.clearCookie(response, "REFRESH_TOKEN");
    }

    @Operation(summary = "토큰 유효 검사")
    @PostMapping("/validate")
    public ResponseEntity<ErrorResponse> validateToken(HttpServletRequest request) {
        String accessToken = extractCookie(request, "ACCESS_TOKEN");
        if (accessToken == null || accessToken.isBlank()) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        authService.validateToken(accessToken);
        return ResponseEntity.ok(ErrorResponse.of(ErrorCode.TOKEN_SUCCESS));
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<TokenReissueResponseDto> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ){
        String refreshToken = extractCookie(request, "REFRESH_TOKEN");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        TokenReissueResponseDto reissued = authService.reissueToken(refreshToken);

        // 새 Access 토큰을 쿠키에 다시 심기 (refresh는 그대로 사용)
        authCookieUtil.addAuthCookies(
                response,
                reissued.getToken(),
                reissued.getRefreshToken()
        );

        // 응답 바디에서는 토큰 제거
        TokenReissueResponseDto body = TokenReissueResponseDto.builder()
                .message(reissued.getMessage())
                .expirationTime(reissued.getExpirationTime())
                .build();

        return ResponseEntity.ok(body);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
