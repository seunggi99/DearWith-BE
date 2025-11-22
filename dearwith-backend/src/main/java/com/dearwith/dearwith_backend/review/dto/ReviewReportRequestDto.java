package com.dearwith.dearwith_backend.review.dto;

import com.dearwith.dearwith_backend.review.enums.ReviewReportReason;

public record ReviewReportRequestDto(
        ReviewReportReason reason,
        String content
) {}
