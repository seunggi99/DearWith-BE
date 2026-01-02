package com.dearwith.dearwith_backend.page.my.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyReviewResponseDto {
    private Long eventId;
    private Long reviewId;

    private String imageUrl;
    private String eventTitle;

    private String reviewContent;
    private Instant createdAt;
    private Instant updatedAt;
}
