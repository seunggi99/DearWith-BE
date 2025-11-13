package com.dearwith.dearwith_backend.review.dto;

public record ReviewLikeResponseDto(
        Long reviewId,
        boolean liked,
        long likeCount
) {}
