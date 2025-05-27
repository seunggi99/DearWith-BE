package com.dearwith.dearwith_backend.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SigninRequestDto {
    private String email;
    private String password;
}
