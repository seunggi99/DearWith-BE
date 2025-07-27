package com.dearwith.dearwith_backend.auth.dto;

import com.dearwith.dearwith_backend.user.enums.AgreementType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgreementDto {

    private AgreementType type;
    private boolean agreed;
}
