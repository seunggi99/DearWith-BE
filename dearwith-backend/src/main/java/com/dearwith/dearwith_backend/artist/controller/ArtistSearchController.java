package com.dearwith.dearwith_backend.artist.controller;

import com.dearwith.dearwith_backend.artist.dto.*;
import com.dearwith.dearwith_backend.artist.service.ArtistUnifiedService;
import com.dearwith.dearwith_backend.artist.service.HotArtistService;
import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
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

@RestController
@RequestMapping("/api/search/artists")
@RequiredArgsConstructor
public class ArtistSearchController {

    private final HotArtistService hotArtistService;
    private final ArtistUnifiedService artistUnifiedService;

    @GetMapping
    @Operation(summary = "아티스트/아티스트 그룹 통합 검색")
    public Page<ArtistUnifiedDto> search(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(name = "query") @NotBlank(message = "검색어를 입력해주세요.") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("nameKr").ascending().and(Sort.by("nameEn").ascending())
        );

        return artistUnifiedService.searchUnified(query, pageable);

    }
    @GetMapping("/artists-groups")
    @Operation(summary = "핫 아티스트/그룹 TOP 20")
    public List<HotArtistDtoResponseDto> getHotArtistsAndGroups() {
        return hotArtistService.getTop20();
    }
}
