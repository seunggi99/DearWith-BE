package com.dearwith.dearwith_backend.user.controller;


import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.user.dto.*;
import com.dearwith.dearwith_backend.user.docs.UserApiDocs;
import com.dearwith.dearwith_backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "회원가입" , description = UserApiDocs.CREATE_DESC)
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponseDto> signUp(@RequestBody @Valid SignUpRequestDto request){
        return ResponseEntity.ok(userService.signUp(request));
    }

    @Operation(summary = "소셜 회원가입", description = UserApiDocs.SOCIAL_CREATE_DESC)
    @PostMapping("/signup/social")
    public ResponseEntity<SignUpResponseDto> kakaoSignUp(@RequestBody @Valid SocialSignUpRequestDto request) {
        SignUpResponseDto response = userService.socialSignUp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "현재 로그인 회원 정보 조회", description = "JWT 토큰으로 받은 현재 로그인한 회원 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }

    @Operation(summary = "회원 닉네임 변경")
    @PatchMapping("/me/nickname")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateNickname(@CurrentUser UUID userId,
                               @RequestBody @Valid UpdateNicknameRequestDto req) {
        userService.updateNickname(userId, req.getNickname());
    }

    @Operation(summary = "비밀번호 재설정 (이메일 인증 필요)")
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody @Valid PasswordResetRequestDto request) {
        userService.resetPassword(request);
    }

    @Operation(summary = "현재 비밀번호 확인", description = "비밀번호 확인 완료 후 5분안에 변경해야합니다.")
    @PostMapping("/me/password/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyPassword(
            @RequestBody @Valid PasswordVerifyRequestDto request,
            @CurrentUser UUID userId
    ) {
        userService.verifyCurrentPassword(request, userId);
    }

    @Operation(summary = "비밀번호 변경 (로그인 필요)",
            description = "비밀번호 확인 완료 후 5분 내, 기존 비밀번호와 다를 시 변경.")
    @PostMapping("/password/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @RequestBody @Valid PasswordChangeRequestDto request,
            @CurrentUser UUID userId
    ) {
        userService.changePassword(request, userId);
    }

    @Operation(summary = "회원 프로필 사진 등록/수정")
    @PatchMapping("/me/profile/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProfileImage(
            @CurrentUser UUID userId,
            @RequestBody ProfileImageUpdateRequestDto request
    ) {
        userService.updateProfileImage(userId, request);
    }

    @Operation(summary = "회원 프로필 사진 삭제")
    @DeleteMapping("/me/profile/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfileImage(
            @CurrentUser UUID userId
    ) {
        userService.deleteProfileImage(userId);
    }

    @Operation(summary = "회원 소프트 삭제(Status 변경)")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@CurrentUser UUID userId) {
        userService.deleteById(userId);
    }

    @Operation(summary = "이메일 중복 검사")
    @GetMapping("/check/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkEmailDuplicate(@RequestParam String email) {
        userService.validateDuplicateUserByEmail(email);
    }

    @Operation(summary = "닉네임 중복 검사")
    @GetMapping("/check/nickname")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkNicknameDuplicate(@RequestParam String nickname) {
        userService.validateDuplicateUserByNickname(nickname);
    }
}
