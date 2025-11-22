package com.dearwith.dearwith_backend.review.dto;


import com.dearwith.dearwith_backend.review.enums.ReviewStatus;

public record ReviewReportResponseDto(
        Long reviewId,
        int reportCount,
        ReviewStatus reviewStatus,
        boolean autoHidden
) {}