package com.dearwith.dearwith_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtTokenDto {
    @NotBlank
    private String token;
}
