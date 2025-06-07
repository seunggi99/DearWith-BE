package com.dearwith.dearwith_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UpdateNicknameRequestDto {
    @NotBlank
    @Size(min = 2, message = "닉네임은 최소 2자 이상이어야 합니다.")
    private String nickname;
}
