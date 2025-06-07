package com.dearwith.dearwith_backend.auth.controller;

import com.dearwith.dearwith_backend.auth.dto.*;
import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.auth.service.EmailVerificationService;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
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


    @Operation(summary = "이메일 인증 코드 발송")
    @PostMapping("/signup/email/send")
    public ResponseEntity<Void> sendCode(@RequestBody @Valid EmailRequestDto request) {
        emailService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "이메일 인증 코드 검증")
    @PostMapping("/signup/email/verify")
    public ResponseEntity<Void> verifyCode(@RequestBody @Valid EmailVerifyRequestDto request) {
        emailService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원가입" , description = "회원 가입시 필요한 Request" +
            "{\n" +
            "  \"email\": \"test@example.com\",\n" +
            "  \"password\": \"testPassword\",\n" +
            "  \"nickname\": \"테스트 닉네임\",\n" +
            "  \"agreements\": [\n" +
            "    {\n" +
            "      \"type\": \"AGE_OVER_14\",\n" +
            "      \"agreed\": true\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"TERMS_OF_SERVICE\",\n" +
            "      \"agreed\": true\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"PERSONAL_INFORMATION\",\n" +
            "      \"agreed\": true\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"MARKETING_CONSENT\",\n" +
            "      \"agreed\": false\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"PUSH_NOTIFICATION\",\n" +
            "      \"agreed\": false\n" +
            "    }\n" +
            "  ]\n" +
            "}")
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponseDto> signUp(@RequestBody @Valid SignUpRequestDto request){
        return ResponseEntity.ok(authService.signUp(request));
    }

    @Operation(summary = "로그인")
    @PostMapping("/signin")
    public ResponseEntity<SignInResponseDto> signIn(@RequestBody @Valid SignInRequestDto request){
        return ResponseEntity.ok(authService.signIn(request));
    }

    @Operation(summary = "토큰 유효 검사")
    @PostMapping("/validate")
    public ResponseEntity<ErrorResponse> validateToken(@RequestBody @Valid JwtTokenDto tokenDto) {
        authService.validateToken(tokenDto);
        return ResponseEntity.ok(ErrorResponse.of(ErrorCode.TOKEN_SUCCESS));
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<TokenReissueResponseDTO> refreshToken(@RequestBody @Valid JwtTokenDto tokenDto){
        return ResponseEntity.ok(authService.reissueToken(tokenDto));
    }

    @Operation(summary = "이메일 중복 검사")
    @GetMapping("/check/email")
    public ResponseEntity<Void> checkEmailDuplicate(@RequestParam String email) {
        authService.validateDuplicateUserByEmail(email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "닉네임 중복 검사")
    @GetMapping("/check/nickname")
    public ResponseEntity<Void> checkNicknameDuplicate(@RequestParam String nickname) {
        authService.validateDuplicateUserByNickname(nickname);
        return ResponseEntity.ok().build();
    }


}
