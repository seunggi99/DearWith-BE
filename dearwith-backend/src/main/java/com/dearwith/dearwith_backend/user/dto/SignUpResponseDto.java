package com.dearwith.dearwith_backend.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignUpResponseDto {
    private String message;
    private String nickname;
}
