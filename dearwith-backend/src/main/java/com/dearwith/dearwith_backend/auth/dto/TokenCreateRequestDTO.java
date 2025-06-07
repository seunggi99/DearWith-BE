package com.dearwith.dearwith_backend.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TokenCreateRequestDTO {
    private UUID userId;
    private String email;
    private String role;
}
