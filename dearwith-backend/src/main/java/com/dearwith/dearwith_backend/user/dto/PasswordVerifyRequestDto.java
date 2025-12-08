package com.dearwith.dearwith_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordVerifyRequestDto {
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;
}
