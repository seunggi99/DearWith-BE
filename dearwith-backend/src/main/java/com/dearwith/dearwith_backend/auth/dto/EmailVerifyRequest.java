package com.dearwith.dearwith_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerifyRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;
}