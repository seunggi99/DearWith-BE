package com.dearwith.dearwith_backend.auth.dto;

import com.dearwith.dearwith_backend.user.domain.Agreement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
public class SignUpRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    @NotBlank
    @Size(min = 2, message = "닉네임은 최소 2자 이상이어야 합니다.")
    private String nickname;

    @Valid
    private List<AgreementDto> agreements;
}
