package com.dearwith.dearwith_backend.user.dto;

import com.dearwith.dearwith_backend.user.enums.AgreementType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AgreementResponseDto {
    private AgreementType type;
    private boolean agreed;
    private LocalDateTime updatedAt;
}
