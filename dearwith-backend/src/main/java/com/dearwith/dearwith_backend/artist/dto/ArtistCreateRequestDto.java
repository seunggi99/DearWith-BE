package com.dearwith.dearwith_backend.artist.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ArtistCreateRequestDto (

        @NotBlank String nameKr,
        String groupId,
        String groupName,
        String imageTmpKey,
        LocalDate birthDate
){}
