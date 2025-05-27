package com.dearwith.dearwith_backend.auth.controller;

import com.dearwith.dearwith_backend.auth.dto.*;
import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.auth.service.EmailVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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
    public ResponseEntity<Void> sendCode(@RequestBody EmailRequest dto) {
        emailService.sendVerificationCode(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    // 2) 인증 코드 검증
    @PostMapping("/signup/email/verify")
    public ResponseEntity<Void> verifyCode(@RequestBody EmailVerifyRequest dto) {
        emailService.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.noContent().build();
    }

    // 3) 최종 회원가입
    @PostMapping("/signup")
    public ResponseEntity<JwtResponse> signUp(@RequestBody SignupRequest signUpRequest){
        return ResponseEntity.ok(authService.signup(signUpRequest));
    }

    // 4) 로그인
    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> signIn(@RequestBody SigninRequestDto dto){
        return ResponseEntity.ok(authService.signIn(dto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody JwtRequest refreshTokenRequest){
        return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest));
    }

    @PostMapping("/validate")
    public ResponseEntity<JwtResponse> validateToken(@RequestBody JwtRequest validateTokenRequest){
        return ResponseEntity.ok(authService.validateToken(validateTokenRequest));
    }

}
