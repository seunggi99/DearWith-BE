package com.dearwith.dearwith_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
public class AgreementDto {
    @NotBlank
    private String type;
    private boolean agreed;
}
