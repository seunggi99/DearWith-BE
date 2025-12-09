package com.dearwith.dearwith_backend.artist.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ArtistGroupCreateRequestDto (

        @NotBlank(message = "이름을 입력해주세요.")
        String nameKr,
        String tmpKey,
        LocalDate debutDate
){}
