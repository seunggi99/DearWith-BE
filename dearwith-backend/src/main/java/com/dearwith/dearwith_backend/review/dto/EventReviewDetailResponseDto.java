package com.dearwith.dearwith_backend.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventReviewDetailResponseDto {

    private Long id;

    private String nickname;
    private String profileImageUrl;

    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<String> tags;

    private int likeCount;

    private boolean liked;
    private boolean editable;
}
