package com.dearwith.dearwith_backend.user.dto;

import com.dearwith.dearwith_backend.auth.dto.AgreementDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SignUpRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.")
    private String password;

    @NotBlank
    @Size(min = 2, message = "닉네임은 최소 2자 이상이어야 합니다.")
    private String nickname;

    @Valid
    private List<AgreementDto> agreements;

    @NotBlank(message = "이메일 인증이 필요합니다.")
    private String emailTicket;
}
