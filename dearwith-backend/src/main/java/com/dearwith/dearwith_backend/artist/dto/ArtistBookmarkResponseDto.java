package com.dearwith.dearwith_backend.artist.dto;

public record ArtistBookmarkResponseDto (
        Long artistId,
        boolean bookmarked,
        long bookmarkCount
) {}
