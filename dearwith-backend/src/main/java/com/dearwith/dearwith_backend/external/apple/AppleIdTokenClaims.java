package com.dearwith.dearwith_backend.external.apple;

public record AppleIdTokenClaims(
        String sub,
        String email,
        boolean emailVerified,
        boolean privateEmail
) {}