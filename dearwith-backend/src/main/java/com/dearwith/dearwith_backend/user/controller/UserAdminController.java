package com.dearwith.dearwith_backend.user.controller;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.user.docs.UserApiDocs;
import com.dearwith.dearwith_backend.user.dto.AdminCreateUserRequestDto;
import com.dearwith.dearwith_backend.user.dto.AdminSuspendUserRequestDto;
import com.dearwith.dearwith_backend.user.dto.SignUpResponseDto;
import com.dearwith.dearwith_backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserService userService;

    @Operation(summary = "관리자용 회원 등록" , description = UserApiDocs.AD_CREATE_DESC)
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponseDto> adminSignUp(@RequestBody AdminCreateUserRequestDto request){
        return ResponseEntity.ok(userService.signUpByAdmin(request));
    }

    @Operation(summary = "회원 정지", description = "관리자가 회원을 정지시킵니다. until이 null이면 무기한 정지입니다.")
    @PatchMapping("/{userId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendUser(
            @CurrentUser UUID adminId,
            @PathVariable UUID userId,
            @RequestBody AdminSuspendUserRequestDto request
    ) {
        userService.suspendUserByAdmin(userId, adminId, request);
    }

    @Operation(summary = "회원 작성 제한", description = "관리자가 회원을 작성 제한시킵니다. until이 null이면 무기한 작성 제한입니다.")
    @PatchMapping("/{userId}/write-restrict")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void writeRestrictUser(
            @CurrentUser UUID adminId,
            @PathVariable UUID userId,
            @RequestBody AdminSuspendUserRequestDto request
    ) {
        userService.writeRestrictUserByAdmin(userId, adminId, request);
    }

    @Operation(summary = "회원 정지 해제", description = "정지 상태의 회원을 다시 활성화합니다.")
    @PatchMapping("/{userId}/unsuspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsuspendUser(
            @CurrentUser UUID adminId,
            @PathVariable UUID userId
    ) {
        userService.unsuspendUserByAdmin(userId, adminId);
    }
}
