package com.dearwith.dearwith_backend.image;

import jakarta.validation.constraints.NotBlank;

public record ImageAttachmentRequest(
        @NotBlank String tmpKey,
        int displayOrder
) {}
