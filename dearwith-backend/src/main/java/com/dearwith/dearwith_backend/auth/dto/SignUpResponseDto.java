package com.dearwith.dearwith_backend.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignUpResponseDto {
    private String message;
    private String nickname;
}
