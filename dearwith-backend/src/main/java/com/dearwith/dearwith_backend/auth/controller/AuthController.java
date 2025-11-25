package com.dearwith.dearwith_backend.auth.controller;

import com.dearwith.dearwith_backend.auth.dto.*;
import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.auth.service.EmailVerificationService;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.exception.ErrorResponse;
import com.dearwith.dearwith_backend.user.dto.SignInRequestDto;
import com.dearwith.dearwith_backend.user.dto.SignInResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final EmailVerificationService emailService;
    private final AuthService authService;
    @Operation(summary = "이메일 인증 코드 발송",
            description = "purpose : SIGNUP(가입), RESET_PASSWORD(비밀번호 변경)")
    @PostMapping("/signup/email/send")
    public ResponseEntity<Void> sendCode(@RequestBody @Valid EmailRequestDto request) {
        emailService.sendVerificationCode(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "이메일 인증 코드 검증",
            description = "purpose : SIGNUP(가입), RESET_PASSWORD(비밀번호 변경)")
    @PostMapping("/signup/email/verify")
    public ResponseEntity<EmailTicketResponseDto> verifyCode(@RequestBody @Valid EmailVerifyRequestDto request) {
        return ResponseEntity.ok(emailService.verifyCode(request));
    }

    @Operation(summary = "로그인")
    @PostMapping("/signin")
    public ResponseEntity<SignInResponseDto> signIn(@RequestBody @Valid SignInRequestDto request){
        return ResponseEntity.ok(authService.signIn(request));
    }

    @PostMapping("/oauth/kakao")
    public ResponseEntity<KakaoSignInResponseDto> kakaoSignIn(@RequestBody KakaoSignInRequestDto request) {
        KakaoSignInResponseDto response = authService.kakaoSignIn(request.getCode());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "토큰 유효 검사")
    @PostMapping("/validate")
    public ResponseEntity<ErrorResponse> validateToken(@RequestBody @Valid JwtTokenDto tokenDto) {
        authService.validateToken(tokenDto);
        return ResponseEntity.ok(ErrorResponse.of(ErrorCode.TOKEN_SUCCESS));
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<TokenReissueResponseDto> refreshToken(@RequestBody @Valid JwtTokenDto tokenDto){
        return ResponseEntity.ok(authService.reissueToken(tokenDto));
    }

}
