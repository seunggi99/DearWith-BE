package com.dearwith.dearwith_backend.auth.dto;

import com.dearwith.dearwith_backend.user.domain.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SignInResponseDto {
    private String message;
    private UUID userId;
    private String nickname;
    private Role role;
    private String token;
    private String refreshToken;
}
