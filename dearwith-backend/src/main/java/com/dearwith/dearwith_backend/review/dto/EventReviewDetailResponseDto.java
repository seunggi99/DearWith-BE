package com.dearwith.dearwith_backend.review.dto;

import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
    private Instant createdAt;
    private Instant updatedAt;

    private List<ImageGroupDto> images;
    private List<String> tags;

    private int likeCount;

    private boolean liked;
    private boolean editable;
}
