package com.dearwith.dearwith_backend.image.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ImageAttachmentUpdateRequestDto(
        Long id,
        @Pattern(regexp = "^tmp/.+", message = "tmpKey는 tmp/로 시작해야 합니다.")
        String tmpKey,

        @NotNull
        Integer displayOrder
){}
