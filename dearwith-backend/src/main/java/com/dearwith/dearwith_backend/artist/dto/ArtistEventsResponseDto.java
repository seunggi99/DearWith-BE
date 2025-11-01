package com.dearwith.dearwith_backend.artist.dto;

import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArtistEventsResponseDto {
    private Long artistId;
    private String artistNameKr;
    private String artistNameEn;
    private Page<EventInfoDto> page;
}
