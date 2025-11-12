package com.dearwith.dearwith_backend.artist.dto;

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
    private String nameEn;
    private String imageUrl;
    private HotArtistType type;
    private long score;

    private LocalDate birthDate;
    private LocalDate debutDate;

    public enum HotArtistType {
        ARTIST,
        GROUP
    }
}
