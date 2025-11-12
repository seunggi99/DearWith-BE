package com.dearwith.dearwith_backend.artist.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistSearchResponseDto {
    // 기존 DTO 그대로 재사용
    private Page<ArtistDto> artists;
    private Page<ArtistGroupDto> groups;
}