package com.dearwith.dearwith_backend.user.controller;


import com.dearwith.dearwith_backend.auth.domain.CustomUserDetails;
import com.dearwith.dearwith_backend.user.domain.User;
import com.dearwith.dearwith_backend.user.dto.UpdateNicknameRequestDto;
import com.dearwith.dearwith_backend.user.dto.UserResponseDto;
import com.dearwith.dearwith_backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // 1) JWT 토큰으로 받은 현재 로그인한 회원 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getId()));
    }

    // 2) 모든 회원 출력
    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // 3) 회원 닉네임 변경
    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname(@AuthenticationPrincipal CustomUserDetails principal,
                                               @RequestBody @Valid UpdateNicknameRequestDto req) {
        userService.updateNickname(principal.getId(), req.getNickname());
        return ResponseEntity.noContent().build();
    }

    // 4) 회원 삭제
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal CustomUserDetails principal) {
        userService.deleteById(principal.getId());
        return ResponseEntity.noContent().build();
    }

}
