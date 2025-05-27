package com.dearwith.dearwith_backend.user.controller;


import com.dearwith.dearwith_backend.user.domain.User;
import com.dearwith.dearwith_backend.user.dto.UpdateNicknameRequest;
import com.dearwith.dearwith_backend.user.dto.UserResponseDto;
import com.dearwith.dearwith_backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // 1) JWT 토큰으로 받은 현재 로그인한 회원 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal User principal) {
        User user = userService.findByEmail(principal.getEmail());
        return ResponseEntity.ok(new UserResponseDto(user));
    }

    // 2) 모든 회원 출력
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> list = userService.findAll().stream()
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // 3) 회원 닉네임 변경
    @PatchMapping("/{id}/nickname")
    public ResponseEntity<Void> updateNickname(
            @AuthenticationPrincipal User principal,
            @RequestBody UpdateNicknameRequest req) {
        userService.updateNickname(principal.getId(), req.getNickname());
        return ResponseEntity.noContent().build();
    }

    // 4) 회원 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
