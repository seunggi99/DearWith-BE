package com.dearwith.dearwith_backend.auth.dto;

import com.dearwith.dearwith_backend.user.dto.EmailVerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class EmailVerifyRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;

    @NotNull
    private EmailVerificationPurpose purpose;
}