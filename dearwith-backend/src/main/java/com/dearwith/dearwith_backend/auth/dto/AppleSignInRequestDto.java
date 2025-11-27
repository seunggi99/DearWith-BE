package com.dearwith.dearwith_backend.auth.dto;

public record AppleSignInRequestDto(
        String authorizationCode,
        String idToken
) {}