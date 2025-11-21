package com.dearwith.dearwith_backend.page.main;

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
public class MainPageReviewDto {

    private Long reviewId;
    private Long eventId;
    private String title;
    private String content;
    private List<ImageGroupDto> images;
}