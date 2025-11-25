package com.dearwith.dearwith_backend.user.dto;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.UUID;

public record EmailVerifyPayload(
        String email,
        EmailVerificationPurpose purpose,
        @Nullable UUID userId,
        Instant issuedAt
) {}
