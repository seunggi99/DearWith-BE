package com.dearwith.dearwith_backend.user.controller;


import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.auth.dto.SignUpRequestDto;
import com.dearwith.dearwith_backend.auth.dto.SignUpResponseDto;
import com.dearwith.dearwith_backend.user.dto.UpdateNicknameRequestDto;
import com.dearwith.dearwith_backend.user.dto.UserResponseDto;
import com.dearwith.dearwith_backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

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
            "      \"type\": \"PUSH_NOTIFICATION\",\n" +
            "      \"agreed\": false\n" +
            "    }\n" +
            "  ]\n" +
            "}")
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponseDto> signUp(@RequestBody @Valid SignUpRequestDto request){
        return ResponseEntity.ok(userService.signUp(request));
    }

    @Operation(summary = "현재 로그인 회원 정보 조회", description = "JWT 토큰으로 받은 현재 로그인한 회원 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getId()));
    }

    @Operation(summary = "가입된 모든 회원 출력", description = "개발용 임시")
    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "회원 닉네임 변경")
    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname(@AuthenticationPrincipal CustomUserDetails principal,
                                               @RequestBody @Valid UpdateNicknameRequestDto req) {
        userService.updateNickname(principal.getId(), req.getNickname());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 소프트 삭제(Status 변경)")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal CustomUserDetails principal) {
        userService.deleteById(principal.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "이메일 중복 검사")
    @GetMapping("/check/email")
    public ResponseEntity<Void> checkEmailDuplicate(@RequestParam String email) {
        userService.validateDuplicateUserByEmail(email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "닉네임 중복 검사")
    @GetMapping("/check/nickname")
    public ResponseEntity<Void> checkNicknameDuplicate(@RequestParam String nickname) {
        userService.validateDuplicateUserByNickname(nickname);
        return ResponseEntity.ok().build();
    }

}
