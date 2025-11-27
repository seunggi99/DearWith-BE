package com.dearwith.dearwith_backend.review.dto;

import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventPhotoReviewResponseDto {
    private List<EventPhotoReviewItemDto> images;
}
