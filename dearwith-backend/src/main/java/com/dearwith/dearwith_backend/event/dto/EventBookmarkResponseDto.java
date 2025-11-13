package com.dearwith.dearwith_backend.event.dto;

public record EventBookmarkResponseDto(
    Long eventId,
    boolean bookmarked,
    long bookmarkCount
) {
}
