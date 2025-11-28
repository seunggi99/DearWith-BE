package com.dearwith.dearwith_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateNicknameRequestDto {
    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, message = "2자 이상 입력해주세요.")
    private String nickname;
}
