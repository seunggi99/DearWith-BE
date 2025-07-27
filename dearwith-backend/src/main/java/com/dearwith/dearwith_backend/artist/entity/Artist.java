package com.dearwith.dearwith_backend.artist.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nameKr; // 아티스트 이름
    private String nameEn;
    private String description; // 아티스트 설명
    private String imageUrl; // 아티스트 이미지 URL
    private LocalDate birthDate; // 아티스트 생년월일
    private LocalDate debutDate;   // 활동 시작일
}
