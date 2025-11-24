package com.dearwith.dearwith_backend.page.main;

import com.dearwith.dearwith_backend.artist.dto.MonthlyAnniversaryDto;
import com.dearwith.dearwith_backend.artist.service.ArtistUnifiedService;
import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.service.EventQueryService;
import com.dearwith.dearwith_backend.image.asset.ImageVariantAssembler;
import com.dearwith.dearwith_backend.image.asset.ImageVariantProfile;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.review.entity.Review;
import com.dearwith.dearwith_backend.review.entity.ReviewImageMapping;
import com.dearwith.dearwith_backend.review.enums.ReviewStatus;
import com.dearwith.dearwith_backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MainPageService {

    private final EventQueryService eventQueryService;
    private final ReviewRepository reviewRepository;
    private final ImageVariantAssembler imageVariantAssembler;
    private final ArtistUnifiedService artistUnifiedService;

    public MainPageResponseDto getMainPage(UUID userId) {

        List<MonthlyAnniversaryDto> birthdayArtists =
                artistUnifiedService.getThisMonthArtistAndGroupAnniversaries();

        List<EventInfoDto> recommendedEvents = eventQueryService.getRecommendedEvents(userId);
        List<EventInfoDto> hotEvents = eventQueryService.getHotEvents(userId);
        List<EventInfoDto> newEvents = eventQueryService.getNewEvents(userId);

        List<MainPageReviewDto> latestReviews = getLatestReviewsForMainPage();

        return MainPageResponseDto.builder()
                .birthdayArtists(birthdayArtists)
                .recommendedEvents(recommendedEvents)
                .hotEvents(hotEvents)
                .newEvents(newEvents)
                .latestReviews(latestReviews)
                .build();
    }

    private List<MainPageReviewDto> getLatestReviewsForMainPage() {

        List<Review> reviews =
                reviewRepository.findTop6ByStatusOrderByIdDesc(ReviewStatus.VISIBLE);

        return reviews.stream()
                .map(this::toMainPageReviewDtoSafely)
                .filter(Objects::nonNull)
                .toList();
    }

    private MainPageReviewDto toMainPageReviewDtoSafely(Review review) {

        if (review.getEvent() == null) {
            return null;
        }

        List<ImageGroupDto> images = List.of();

        if (review.getImages() != null && !review.getImages().isEmpty()) {
            images = review.getImages().stream()
                    .sorted(Comparator.comparingInt(ReviewImageMapping::getDisplayOrder))
                    .map(m -> {
                        Image img = m.getImage();
                        if (img == null || img.getImageUrl() == null) return null;

                        return ImageGroupDto.builder()
                                .id(img.getId())
                                .variants(
                                        imageVariantAssembler.toVariants(
                                                img.getImageUrl(),
                                                ImageVariantProfile.MAIN_REVIEW_THUMB
                                        )
                                )
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

        return MainPageReviewDto.builder()
                .reviewId(review.getId())
                .eventId(review.getEvent().getId())
                .title(review.getEvent().getTitle())
                .content(review.getContent())
                .images(images)
                .build();
    }
}