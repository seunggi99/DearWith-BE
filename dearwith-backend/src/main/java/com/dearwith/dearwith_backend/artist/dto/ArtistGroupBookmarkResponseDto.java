package com.dearwith.dearwith_backend.artist.dto;

public record ArtistGroupBookmarkResponseDto(
        Long artistGroupId,
        boolean bookmarked,
        long bookmarkCount
) {}
