package com.dearwith.dearwith_backend.artist.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ArtistInfoDto {
    private Long id;
    private String nameKo;
    private String nameEn;
    private String imageUrl;

    private LocalDate birthDate; // 아티스트 생년월일
    private LocalDate debutDate; // 활동 시작일

    private boolean isBirthday; // 오늘 생일 여부
}
