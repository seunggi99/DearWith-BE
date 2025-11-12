package com.dearwith.dearwith_backend.artist.controller;

import com.dearwith.dearwith_backend.artist.dto.ArtistDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistGroupDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistSearchResponseDto;
import com.dearwith.dearwith_backend.artist.dto.HotArtistDtoResponseDto;
import com.dearwith.dearwith_backend.artist.service.ArtistGroupService;
import com.dearwith.dearwith_backend.artist.service.ArtistService;
import com.dearwith.dearwith_backend.artist.service.HotArtistService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search/artists")
@RequiredArgsConstructor
public class ArtistSearchController {
    private final ArtistService artistService;
    private final ArtistGroupService artistGroupService;
    private final HotArtistService hotArtistService;

    @GetMapping
    @Operation(summary = "아티스트/아티스트 그룹 통합 검색")
    public ArtistSearchResponseDto search(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestParam(name = "query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("nameKr").ascending().and(Sort.by("nameEn").ascending())
        );

        Page<ArtistDto> artistPage = artistService.search(query, pageable);
        Page<ArtistGroupDto> groupPage  = artistGroupService.search(query, pageable);

        return ArtistSearchResponseDto.builder()
                .artists(artistPage)
                .groups(groupPage)
                .build();
    }
    @GetMapping("/artists-groups")
    @Operation(summary = "핫 아티스트/그룹 TOP 20")
    public List<HotArtistDtoResponseDto> getHotArtistsAndGroups() {
        return hotArtistService.getTop20();
    }
}
