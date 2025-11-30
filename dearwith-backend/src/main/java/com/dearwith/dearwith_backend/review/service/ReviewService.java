package com.dearwith.dearwith_backend.review.service;

import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.event.service.HotEventService;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import com.dearwith.dearwith_backend.image.asset.ImageVariantAssembler;
import com.dearwith.dearwith_backend.image.asset.ImageVariantProfile;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.page.my.dto.MyReviewResponseDto;
import com.dearwith.dearwith_backend.review.dto.*;
import com.dearwith.dearwith_backend.review.entity.Review;
import com.dearwith.dearwith_backend.review.entity.ReviewImageMapping;
import com.dearwith.dearwith_backend.review.entity.ReviewLike;
import com.dearwith.dearwith_backend.review.repository.ReviewImageMappingRepository;
import com.dearwith.dearwith_backend.review.repository.ReviewLikeRepository;
import com.dearwith.dearwith_backend.review.repository.ReviewRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final ReviewImageMappingRepository reviewImageMappingRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewImageAppService reviewImageAppService;
    private final AuthService authService;
    private final HotEventService hotEventService;
    private final ImageVariantAssembler imageVariantAssembler;
    private final AssetUrlService assetUrlService;
    private final UserReader userReader;

    /*──────────────────────────────────────────────
     | 1. 리뷰 생성
     *──────────────────────────────────────────────*/
    @Transactional
    public void create(UUID userId, Long eventId, ReviewCreateRequestDto req) {

        String normalizedContent = Normalizer.normalize(req.content().trim(), Normalizer.Form.NFC);

        // 1) 유저, 이벤트 조회
        User user = userReader.getActiveUser(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "이벤트를 찾을 수 없습니다.",
                        "EVENT_NOT_FOUND"
                ));

        // 2) 리뷰 엔티티 생성
        Review review = Review.builder()
                .user(user)
                .event(event)
                .content(normalizedContent)
                .build();

        // 태그 추가
        if (req.tags() != null) {
            req.tags().forEach(review::addTag);
        }

        // 3) 저장
        Review saved = reviewRepository.save(review);

        // 4) 이미지 첨부
        if (req.images() != null && !req.images().isEmpty()) {
            List<ImageAttachmentRequestDto> imageDtos = req.images().stream()
                    .map(d -> new ImageAttachmentRequestDto(d.tmpKey(), d.displayOrder()))
                    .toList();
            reviewImageAppService.create(saved, imageDtos, user);
        }

        // 5) 핫 이벤트 점수 반영
        hotEventService.increaseEventScore(eventId, HotEventService.Action.REVIEW);
    }

    /*──────────────────────────────────────────────
     | 2. 이벤트 포토 리뷰 썸네일 목록
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public EventPhotoReviewResponseDto getEventPhotoReviews(Long eventId, Pageable pageable) {
        Page<ReviewImageMapping> mappings =
                reviewImageMappingRepository.findVisibleLatestByEvent(eventId, pageable);

        List<EventPhotoReviewItemDto> items = mappings.stream()
                .map(rim -> {
                    Image img = rim.getImage();

                    ImageGroupDto imageGroup = ImageGroupDto.builder()
                            .id(img.getId())
                            .variants(
                                    imageVariantAssembler.toVariants(
                                            assetUrlService.generatePublicUrl(img),
                                            ImageVariantProfile.REVIEW_PHOTO
                                    )
                            )
                            .build();

                    return EventPhotoReviewItemDto.builder()
                            .reviewId(
                                    rim.getReview() != null ? rim.getReview().getId() : null
                            )
                            .image(imageGroup)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();


        return EventPhotoReviewResponseDto.builder()
                .images(items)
                .build();
    }

    /*──────────────────────────────────────────────
     | 3. 이벤트별 리뷰 목록 (최신/인기 정렬)
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public Page<EventReviewResponseDto> getReviewsByEvent(Long eventId, UUID userId, Pageable pageable) {
        // 1) ID 페이징
        Page<Long> idPage = reviewRepository.findIdsByEvent(eventId, pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2) 상세 fetch (작성자 + 이미지)
        List<Long> orderedIds = idPage.getContent();

        Set<Long> likedIdSet = Collections.emptySet();
        if (userId != null) {
            userReader.getLoginAllowedUser(userId);
            likedIdSet = new HashSet<>(reviewLikeRepository.findLikedReviewIds(userId, orderedIds));
        }

        List<Review> fetched = reviewRepository.findWithUserAndImagesByIdIn(orderedIds);
        reviewRepository.preloadTagsByReviewIds(orderedIds);

        Map<Long, Review> byId = new HashMap<>(fetched.size());
        for (Review r : fetched) {
            byId.put(r.getId(), r);
        }

        // 3) 원래 순서대로 DTO 매핑
        List<EventReviewResponseDto> dtoList = new ArrayList<>(orderedIds.size());
        for (Long id : orderedIds) {
            Review r = byId.get(id);
            if (r == null) continue;

            boolean liked = (userId != null) && likedIdSet.contains(id);
            dtoList.add(mapToEventReviewResponseDto(r, userId, liked));
        }

        return new PageImpl<>(dtoList, pageable, idPage.getTotalElements());
    }

    private EventReviewResponseDto mapToEventReviewResponseDto(Review r, UUID userId, boolean liked) {
        User user = r.getUser();

        String profileImageUrl = null;
        try {
            profileImageUrl = (user != null && user.getProfileImage() != null)
                    ? assetUrlService.generatePublicUrl(user.getProfileImage())
                    : null;
        } catch (Exception ignore) {
        }

        List<ImageGroupDto> images =
                (r.getImages() == null)
                        ? List.of()
                        : r.getImages().stream()
                        .sorted(Comparator.comparingInt(ReviewImageMapping::getDisplayOrder))
                        .map(m -> {
                            Image img = m.getImage();

                            return ImageGroupDto.builder()
                                    .id(img.getId())
                                    .variants(
                                            imageVariantAssembler.toVariants(
                                                    assetUrlService.generatePublicUrl(img),
                                                    ImageVariantProfile.REVIEW_LIST
                                            )
                                    )
                                    .build();
                        })
                        .filter(Objects::nonNull)
                        .toList();

        List<String> tags = (r.getTags() != null) ? r.getTags() : List.of();

        boolean editable = (user != null && user.getId() != null && user.getId().equals(userId));

        return EventReviewResponseDto.builder()
                .id(r.getId())
                .nickname(user != null ? user.getNickname() : null)
                .profileImageUrl(profileImageUrl)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .content(r.getContent())
                .images(images)
                .tags(tags)
                .likeCount(r.getLikeCount() == null ? 0 : r.getLikeCount())
                .liked(liked)
                .editable(editable)
                .build();
    }

    /*──────────────────────────────────────────────
     | 4. 리뷰 좋아요
     *──────────────────────────────────────────────*/
    @Transactional
    public ReviewLikeResponseDto like(Long reviewId, UUID userId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "리뷰를 찾을 수 없습니다.",
                        "REVIEW_NOT_FOUND"
                ));

        User user = userReader.getLoginAllowedUser(userId);

        try {
            reviewLikeRepository.save(
                    ReviewLike.builder()
                            .review(review)
                            .user(user)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ALREADY_LIKED,
                    "이미 좋아요를 누른 리뷰입니다.",
                    "REVIEW_ALREADY_LIKED"
            );
        }

        reviewRepository.incrementLike(reviewId);
        Integer likeCount = reviewRepository.getLikeCount(reviewId);

        return new ReviewLikeResponseDto(
                reviewId,
                true,
                likeCount
        );
    }

    /*──────────────────────────────────────────────
     | 5. 리뷰 좋아요 취소
     *──────────────────────────────────────────────*/
    @Transactional
    public ReviewLikeResponseDto unlike(Long reviewId, UUID userId) {
        userReader.getLoginAllowedUser(userId);
        ReviewLike like = reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "좋아요를 찾을 수 없습니다.",
                        "REVIEW_LIKE_NOT_FOUND"
                ));

        reviewLikeRepository.delete(like);
        reviewRepository.decrementLike(like.getReview().getId());

        Integer likeCount = reviewRepository.getLikeCount(reviewId);

        return new ReviewLikeResponseDto(
                reviewId,
                false,
                likeCount
        );
    }

    /*──────────────────────────────────────────────
     | 6. 리뷰 수정
     *──────────────────────────────────────────────*/
    @Transactional
    public void update(UUID userId, Long reviewId, ReviewUpdateRequestDto req) {
        User user = userReader.getActiveUser(userId);
        Review review = reviewRepository.findByIdAndUserIdWithTags(reviewId, userId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "리뷰를 찾을 수 없거나 권한이 없습니다.",
                        "REVIEW_NOT_FOUND_OR_NOT_OWNER"
                ));

        authService.validateOwner(review.getUser(), user, "리뷰를 수정할 권한이 없습니다.");

        // 1) content
        if (req.content() != null) {
            String c = req.content();
            if (c.isBlank()) {
                review.setContent(null);
            } else {
                String normalized = Normalizer.normalize(c, Normalizer.Form.NFC);
                review.setContent(normalized);
            }
        }

        // 2) tags
        if (req.tags() != null) {
            if (req.tags().isEmpty()) {
                review.getTags().clear();
            } else {
                List<String> desired = req.tags().stream()
                        .map(s -> s == null ? "" : s.trim())
                        .filter(s -> !s.isEmpty())
                        .map(s -> Normalizer.normalize(s, Normalizer.Form.NFC))
                        .limit(4)
                        .toList();

                review.getTags().clear();
                review.getTags().addAll(desired);
            }
        }

        // 3) images
        if (req.images() != null) {
            if (req.images().isEmpty()) {
                reviewImageAppService.deleteAll(reviewId);
            } else {
                reviewImageAppService.update(review, req.images(), userId);
            }
        }

        reviewRepository.save(review);
    }

    /*──────────────────────────────────────────────
     | 7. 리뷰 삭제 (Soft delete)
     *──────────────────────────────────────────────*/
    @Transactional
    public void delete(Long reviewId, UUID userId) {
        User user = userReader.getLoginAllowedUser(userId);
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "리뷰를 찾을 수 없거나 권한이 없습니다.",
                        "REVIEW_NOT_FOUND_OR_NOT_OWNER"
                ));

        authService.validateOwner(review.getUser(), user, "리뷰를 삭제할 권한이 없습니다.");

        if (review.getTags() != null && !review.getTags().isEmpty()) {
            review.getTags().clear();
        }

        reviewImageAppService.deleteAll(reviewId);
        reviewLikeRepository.deleteByReviewId(reviewId);

        review.softDelete();
        reviewRepository.save(review);

        hotEventService.decreaseEventScore(review.getEvent().getId(), HotEventService.Action.REVIEW);
    }

    /*──────────────────────────────────────────────
     | 8. 리뷰 상세(이미지 제외)
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public EventReviewDetailResponseDto getEventReviewDetail(
            Long reviewId,
            UUID userId
    ) {
        Review r = reviewRepository.findWithUserById(reviewId)
                .orElseThrow(() -> BusinessException.withMessage(ErrorCode.NOT_FOUND,"리뷰를 찾을 수 없습니다."));


        User viewer = null;
        if (userId != null) {
            viewer = userReader.getLoginAllowedUser(userId);
        }

        boolean liked = (viewer != null)
                && reviewLikeRepository.existsByReviewIdAndUserId(reviewId, viewer.getId());

        boolean editable = (viewer != null)
                && r.getUser() != null
                && viewer.getId().equals(r.getUser().getId());

        return EventReviewDetailResponseDto.builder()
                .id(r.getId())
                .nickname(r.getUser() != null ? r.getUser().getNickname() : null)
                .profileImageUrl(
                        r.getUser().getProfileImage() != null
                                ? assetUrlService.generatePublicUrl(r.getUser().getProfileImage())
                                : null
                )
                .content(r.getContent())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .tags(
                        r.getTags() == null
                                ? List.of()
                                : List.copyOf(r.getTags())
                )                .likeCount(r.getLikeCount())
                .liked(liked)
                .editable(editable)
                .build();
    }

    /*──────────────────────────────────────────────
    | 9. 내가 작성한 리뷰 (마이페이지)
    *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public Page<MyReviewResponseDto> getMyReviews(
            UUID userId,
            int page,
            int size,
            Integer months
    ) {
        User user = userReader.getLoginAllowedUser(userId);

        int monthValue = (months == null || months < 1) ? 1 : months;
        LocalDateTime from = LocalDateTime.now().minusMonths(monthValue);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Review> reviewPage = reviewRepository.findByUser_IdAndCreatedAtAfter(user.getId(), from, pageable);

        return reviewPage.map(this::toMyReviewDto);
    }

    private MyReviewResponseDto toMyReviewDto(Review review) {
        Event event = review.getEvent();

        String imageUrl = null;
        if (event != null && event.getCoverImage() != null) {
            try {
                imageUrl = assetUrlService.generatePublicUrl(event.getCoverImage());
            } catch (Exception ignore) {
            }
        }

        return MyReviewResponseDto.builder()
                .eventId(event != null ? event.getId() : null)
                .reviewId(review.getId())
                .imageUrl(imageUrl)
                .eventTitle(event != null ? event.getTitle() : null)
                .reviewContent(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}