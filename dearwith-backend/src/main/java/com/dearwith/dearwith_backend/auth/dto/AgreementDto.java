package com.dearwith.dearwith_backend.auth.dto;

import com.dearwith.dearwith_backend.user.enums.AgreementType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgreementDto {

    @NotNull(message = "동의 유형은 필수입니다.")
    private AgreementType type;

    @NotNull(message = "동의 여부는 필수입니다.")
    private boolean agreed;
}
