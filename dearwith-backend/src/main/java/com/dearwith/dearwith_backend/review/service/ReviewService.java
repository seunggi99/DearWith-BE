package com.dearwith.dearwith_backend.review.service;


import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.common.dto.ImageVariantDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.image.Image;
import com.dearwith.dearwith_backend.image.ImageAttachmentRequest;
import com.dearwith.dearwith_backend.image.ImageAttachmentService;
import com.dearwith.dearwith_backend.review.dto.EventPhotoReviewResponseDto;
import com.dearwith.dearwith_backend.review.dto.EventReviewResponseDto;
import com.dearwith.dearwith_backend.review.dto.ReviewCreateRequestDto;
import com.dearwith.dearwith_backend.review.entity.Review;
import com.dearwith.dearwith_backend.review.entity.ReviewImageMapping;
import com.dearwith.dearwith_backend.review.entity.ReviewLike;
import com.dearwith.dearwith_backend.review.repository.ReviewImageMappingRepository;
import com.dearwith.dearwith_backend.review.repository.ReviewLikeRepository;
import com.dearwith.dearwith_backend.review.repository.ReviewRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ImageAttachmentService imageAttachmentService;
    private final ReviewImageMappingRepository reviewImageMappingRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    @Transactional
    public Long createReview(UUID userId, Long eventId, ReviewCreateRequestDto req) {
        // 0) 유효성
        if (req.content() == null || req.content().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "리뷰 내용은 비어 있을 수 없습니다.");
        }
        if (req.content().length() > 300) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "리뷰는 300자 이하만 가능합니다.");
        }
        if (req.tags() != null && req.tags().size() > 4) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "태그는 최대 4개까지 가능합니다.");
        }
        if (req.images() != null && req.images().size() > 2) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지는 최대 2개까지만 등록할 수 있습니다.");
        }

        // 1) 유저, 이벤트 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "유저를 찾을 수 없습니다."));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이벤트를 찾을 수 없습니다."));

        // 2) 리뷰 엔티티 생성
        Review review = Review.builder()
                .user(user)
                .event(event)
                .content(req.content().trim())
                .build();

        // 태그 추가
        if (req.tags() != null) {
            req.tags().forEach(review::addTag);
        }

        // 3) 저장
        Review saved = reviewRepository.save(review);

        // 4) 이미지 첨부
        if (req.images() != null && !req.images().isEmpty()) {
            List<ImageAttachmentRequest> imageDtos = req.images().stream()
                    .map(d -> new ImageAttachmentRequest(d.tmpKey(), d.displayOrder()))
                    .toList();
            imageAttachmentService.setReviewImages(saved, imageDtos, userId);
        }

        return saved.getId();
    }

    public EventPhotoReviewResponseDto getEventPhotoReviews(Long eventId, Pageable pageable) {
        Page<ReviewImageMapping> mappings =
                reviewImageMappingRepository.findVisibleLatestByEvent(eventId, pageable);

        List<ImageGroupDto> imageGroups = mappings.stream()
                .map(rim -> {
                    Image img = rim.getImage();

                    String baseUrl = img.getImageUrl();

                    int lastSlash = baseUrl.lastIndexOf('/');
                    String prefix = baseUrl.substring(0, lastSlash + 1);
                    String filename = baseUrl.substring(lastSlash + 1);
                    String stem = filename.substring(0, filename.lastIndexOf('.'));

                    List<ImageVariantDto> variants = List.of(
                            ImageVariantDto.builder()
                                    .name("original")
                                    .url(baseUrl)
                                    .build(),
                            ImageVariantDto.builder()
                                    .name("large")
                                    .url(prefix + stem + "/large.webp")
                                    .build(),
                            ImageVariantDto.builder()
                                    .name("photo@1x")
                                    .url(prefix + stem + "/photo@1x.webp")
                                    .build(),
                            ImageVariantDto.builder()
                                    .name("photo@2x")
                                    .url(prefix + stem + "/photo@2x.webp")
                                    .build()
                    );

                    return ImageGroupDto.builder()
                            .group(stem)
                            .variants(variants)
                            .build();
                })
                .toList();

        return EventPhotoReviewResponseDto.builder()
                .images(imageGroups)
                .build();
    }

    /**
     * 이벤트별 리뷰 페이지 조회 (최신/인기 정렬은 Pageable.sort로 전달)
     * - 1차: ID 페이징 (가볍게)
     * - 2차: ID 모음으로 작성자/이미지 fetch join
     * - 3차: 원래 ID 순서 유지하여 DTO 매핑
     */
    @Transactional(readOnly = true)
    public Page<EventReviewResponseDto> getReviewsByEvent(Long eventId, UUID userId, Pageable pageable) {
        // 1) ID만 가볍게 페이징(정렬 포함)
        Page<Long> idPage = reviewRepository.findIdsByEvent(eventId, pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2) 상세 fetch (작성자 + 이미지 일괄 로딩)
        List<Long> orderedIds = idPage.getContent();

        Set<Long> likedIdSet = Collections.emptySet();
        if (userId != null) {
            likedIdSet = new HashSet<>(reviewLikeRepository.findLikedReviewIds(userId, orderedIds));
        }

        List<Review> fetched = reviewRepository.findWithUserAndImagesByIdIn(orderedIds);

        // 2-1) id -> Review 매핑
        Map<Long, Review> byId = new HashMap<>(fetched.size());
        for (Review r : fetched) byId.put(r.getId(), r);

        // 3) 원래 ID 순서대로 DTO 변환
        List<EventReviewResponseDto> dtoList = new ArrayList<>(orderedIds.size());
        for (Long id : orderedIds) {
            Review r = byId.get(id);
            if (r == null) continue;

            boolean liked = (userId != null) && likedIdSet.contains(id);
            EventReviewResponseDto dto = mapToEventReviewResponseDto(r, userId, liked);
            dtoList.add(dto);
        }

        return new PageImpl<>(dtoList, pageable, idPage.getTotalElements());
    }

    /**
     * Review -> EventReviewResponseDto 매핑
     * - 작성자 닉네임/프로필, 작성시간, 내용, 태그(없으면 빈 리스트), 좋아요 수, 본인 수정 가능 여부
     * - 리뷰 이미지: displayOrder 오름차순으로 정렬하여 노출
     *
     * 프로필 이미지나 태그 컬럼/연관관계 이름은 프로젝트에 맞게 수정하세요.
     */
    private EventReviewResponseDto mapToEventReviewResponseDto(Review r, UUID userId, boolean liked) {
        var user = r.getUser();

        String profileImageUrl = null;
        try {
            profileImageUrl = user.getProfileImage() != null ? user.getProfileImage().getImageUrl() : null;
        } catch (Exception ignore) {}


        List<ImageGroupDto> images = r.getImages() == null
                ? List.of()
                : r.getImages().stream()
                .sorted(Comparator.comparingInt(ReviewImageMapping::getDisplayOrder))
                .map(m -> {
                    var baseUrl = (m.getImage() != null) ? m.getImage().getImageUrl() : null;
                    if (baseUrl == null) return null;

                    String prefix = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1);
                    String stem = baseUrl.substring(baseUrl.lastIndexOf('/') + 1, baseUrl.lastIndexOf('.'));

                    return ImageGroupDto.builder()
                            .group(stem)
                            .variants(List.of(
                                    new ImageVariantDto("original", baseUrl),
                                    new ImageVariantDto("large", prefix + stem + "/large.webp"),
                                    new ImageVariantDto("review@1x", prefix + stem + "/review@1x.webp"),
                                    new ImageVariantDto("review@2x", prefix + stem + "/review@2x.webp")
                            ))
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        List<String> tags = r.getTags() != null ? r.getTags() : List.of();

        boolean editable = (user != null && user.getId() != null && user.getId().equals(userId));

        return EventReviewResponseDto.builder()
                .id(r.getId())
                .nickname(user != null ? user.getNickname() : null)
                .profileImageUrl(profileImageUrl)
                .createdAt(r.getCreatedAt())
                .content(r.getContent())
                .images(images)
                .tags(tags)
                .likeCount(r.getLikeCount() == null ? 0 : r.getLikeCount())
                .liked(liked)
                .editable(editable)
                .build();
    }

    @Transactional
    public void like(Long reviewId, UUID userId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        try {
            reviewLikeRepository.save(
                    ReviewLike.builder()
                            .review(review)
                            .user(user)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        reviewRepository.incrementLike(reviewId);
    }

    @Transactional
    public void unlike(Long reviewId, UUID userId) {
        ReviewLike like = reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "좋아요를 찾을 수 없습니다."));

        reviewLikeRepository.delete(like);
        reviewRepository.decrementLike(like.getReview().getId());
    }
}
