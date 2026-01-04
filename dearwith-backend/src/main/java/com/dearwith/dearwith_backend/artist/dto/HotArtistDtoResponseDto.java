package com.dearwith.dearwith_backend.artist.dto;

import com.dearwith.dearwith_backend.artist.enums.ArtistType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HotArtistDtoResponseDto {

    private Long id;
    private String nameKr;
    private String imageUrl;
    private ArtistType type;
    private long score;

    private LocalDate birthDate;
    private LocalDate debutDate;
}
