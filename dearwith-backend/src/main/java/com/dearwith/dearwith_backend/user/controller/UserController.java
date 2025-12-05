package com.dearwith.dearwith_backend.user.controller;


import com.dearwith.dearwith_backend.user.dto.*;
import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.user.docs.UserApiDocs;
import com.dearwith.dearwith_backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;



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
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getId()));
    }

    @Operation(summary = "가입된 모든 회원 출력", description = "개발용 임시")
    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "회원 닉네임 변경")
    @PatchMapping("/me/nickname")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateNickname(@AuthenticationPrincipal CustomUserDetails principal,
                                               @RequestBody @Valid UpdateNicknameRequestDto req) {
        userService.updateNickname(principal.getId(), req.getNickname());
    }

    @Operation(summary = "비밀번호 변경 (이메일 인증 필요)")
    @PostMapping("/password/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody @Valid PasswordChangeRequestDto request) {
        userService.changePassword(request);
    }

    @Operation(summary = "회원 프로필 사진 등록/수정")
    @PatchMapping("/me/profile/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProfileImage(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestBody ProfileImageUpdateRequestDto request
    ) {
        userService.updateProfileImage(principal.getId(), request);
    }

    @Operation(summary = "회원 프로필 사진 삭제")
    @DeleteMapping("/me/profile/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfileImage(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        userService.deleteProfileImage(principal.getId());
    }

    @Operation(summary = "회원 소프트 삭제(Status 변경)")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@AuthenticationPrincipal CustomUserDetails principal) {
        userService.deleteById(principal.getId());
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
