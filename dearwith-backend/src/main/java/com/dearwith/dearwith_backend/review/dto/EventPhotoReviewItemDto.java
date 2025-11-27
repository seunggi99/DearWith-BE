package com.dearwith.dearwith_backend.review.dto;

import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventPhotoReviewItemDto {
    private Long reviewId;
    private ImageGroupDto image;
}
