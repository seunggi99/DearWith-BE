package com.dearwith.dearwith_backend.page.main;

import com.dearwith.dearwith_backend.artist.dto.ArtistInfoDto;
import com.dearwith.dearwith_backend.banner.BannerDto;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
public class MainPageResponseDto {
    private List<BannerDto> banners;
    private List<ArtistInfoDto> birthdayArtists;     // 오늘 생일 아티스트
    private List<EventInfoDto> recommendedEvents;    // 추천 이벤트
    private List<EventInfoDto> hotEvents;            // HOT 이벤트
    private List<EventInfoDto> newEvents;            // 최신 등록 이벤트
    private List<MainPageReviewDto> latestReviews;   // 최신 리뷰 6개
}
