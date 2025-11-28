package com.dearwith.dearwith_backend.auth.dto;


import com.dearwith.dearwith_backend.user.dto.EmailVerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailRequestDto {
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email
    private String email;

    @NotNull
    private EmailVerificationPurpose purpose;
}