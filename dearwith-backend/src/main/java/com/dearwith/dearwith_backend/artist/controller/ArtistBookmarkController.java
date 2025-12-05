package com.dearwith.dearwith_backend.artist.controller;


import com.dearwith.dearwith_backend.artist.dto.*;
import com.dearwith.dearwith_backend.artist.service.ArtistUnifiedService;
import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistBookmarkController {

    private final ArtistUnifiedService artistUnifiedBookmarkService;

    @Operation(summary = "북마크한 아티스트/그룹 조회")
    @GetMapping("/bookmark")
    public Page<ArtistUnifiedDto> getBookmarkedArtists(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return artistUnifiedBookmarkService.getBookmarkedArtistsAndGroups(principal.getId(), pageable);
    }

    @Operation(summary = "아티스트 북마크 추가")
    @PostMapping("/{artistId}/bookmark")
    public ArtistBookmarkResponseDto addArtistBookmark(
            @PathVariable Long artistId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        return artistUnifiedBookmarkService.addArtistBookmark(artistId, principal.getId());
    }

    @Operation(summary = "아티스트 북마크 해제")
    @DeleteMapping("/{artistId}/bookmark")
    public ArtistBookmarkResponseDto removeArtistBookmark(
            @PathVariable Long artistId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        return artistUnifiedBookmarkService.removeArtistBookmark(artistId, principal.getId());
    }

    @Operation(summary = "아티스트 그룹 북마크 추가")
    @PostMapping("/groups/{groupId}/bookmark")
    public ArtistGroupBookmarkResponseDto addGroupBookmark(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        return artistUnifiedBookmarkService.addArtistGroupBookmark(groupId, principal.getId());
    }

    @Operation(summary = "아티스트 그룹 북마크 해제")
    @DeleteMapping("/groups/{groupId}/bookmark")
    public ArtistGroupBookmarkResponseDto removeGroupBookmark(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        return artistUnifiedBookmarkService.removeArtistGroupBookmark(groupId, principal.getId());
    }
}
