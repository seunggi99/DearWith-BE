package com.dearwith.dearwith_backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignInRequestDto {
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email
    @Schema(description = "이메일", example = "test@test")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Schema(description = "비밀번호", example = "test1234!")
    private String password;
}
