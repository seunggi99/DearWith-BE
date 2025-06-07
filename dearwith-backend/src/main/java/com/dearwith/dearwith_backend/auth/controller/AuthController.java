package com.dearwith.dearwith_backend.auth.controller;

import com.dearwith.dearwith_backend.auth.dto.*;
import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.auth.service.EmailVerificationService;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.exception.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private final EmailVerificationService emailService;

    @Autowired
    private AuthService authService;

    public AuthController(EmailVerificationService emailService) {
        this.emailService = emailService;
    }


    // 1) 인증 코드 발송
    @PostMapping("/signup/email/send")
    public ResponseEntity<Void> sendCode(@RequestBody @Valid EmailRequestDto request) {
        emailService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok().build();
    }

    // 2) 인증 코드 검증
    @PostMapping("/signup/email/verify")
    public ResponseEntity<Void> verifyCode(@RequestBody @Valid EmailVerifyRequestDto request) {
        emailService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.noContent().build();
    }

    // 3) 회원가입
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponseDto> signUp(@RequestBody @Valid SignUpRequestDto request){
        return ResponseEntity.ok(authService.signUp(request));
    }

    // 4) 로그인
    @PostMapping("/signin")
    public ResponseEntity<SignInResponseDto> signIn(@RequestBody @Valid SignInRequestDto request){
        return ResponseEntity.ok(authService.signIn(request));
    }

    // 토큰 유효 검사
    @PostMapping("/validate")
    public ResponseEntity<ErrorResponse> validateToken(@RequestBody @Valid JwtTokenDto tokenDto) {
        authService.validateToken(tokenDto);
        return ResponseEntity.ok(ErrorResponse.of(ErrorCode.TOKEN_SUCCESS));
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenReissueResponseDTO> refreshToken(@RequestBody @Valid JwtTokenDto tokenDto){
        return ResponseEntity.ok(authService.reissueToken(tokenDto));
    }

}
