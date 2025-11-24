package com.dearwith.dearwith_backend.user.dto;

import com.dearwith.dearwith_backend.auth.dto.AgreementDto;
import com.dearwith.dearwith_backend.user.enums.AuthProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoSignUpRequestDto {

    @NotBlank(message = "소셜 제공자가 필요합니다.")
    private AuthProvider provider;

    @NotBlank(message = "소셜 ID가 필요합니다.")
    private String socialId;

    @NotBlank
    @Size(min = 2, message = "닉네임은 최소 2자 이상이어야 합니다.")
    private String nickname;

    @Valid
    private List<AgreementDto> agreements;
}