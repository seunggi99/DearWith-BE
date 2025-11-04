package com.dearwith.dearwith_backend.review.service;


import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.image.ImageAttachmentRequest;
import com.dearwith.dearwith_backend.image.ImageAttachmentService;
import com.dearwith.dearwith_backend.review.dto.ReviewCreateRequestDto;
import com.dearwith.dearwith_backend.review.entity.Review;
import com.dearwith.dearwith_backend.review.repository.ReviewRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ImageAttachmentService imageAttachmentService;

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
}
