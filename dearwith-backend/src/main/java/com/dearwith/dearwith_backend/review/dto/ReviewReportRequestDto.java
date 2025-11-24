package com.dearwith.dearwith_backend.review.dto;

import com.dearwith.dearwith_backend.review.enums.ReviewReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewReportRequestDto(

        @NotNull(message = "신고 사유는 필수입니다.")
        ReviewReportReason reason,

        @NotNull(message = "신고 내용은 필수입니다.")
        @Size(max = 300, message = "신고 내용은 최대 300자까지 입력할 수 있습니다.")
        String content
) {}
