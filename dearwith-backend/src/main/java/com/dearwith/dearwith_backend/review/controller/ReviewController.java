package com.dearwith.dearwith_backend.review.controller;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.review.docs.ReviewApiDocs;
import com.dearwith.dearwith_backend.review.dto.*;
import com.dearwith.dearwith_backend.review.enums.ReviewSort;
import com.dearwith.dearwith_backend.review.service.ReviewReportService;
import com.dearwith.dearwith_backend.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ReviewController {
    private final ReviewService reviewService;
    private final ReviewReportService reviewReportService;
    @PostMapping("/api/events/{eventId}/reviews")
    @Operation(summary = "이벤트 리뷰 작성")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @PathVariable Long eventId,
            @CurrentUser UUID userId,
            @Valid @RequestBody ReviewCreateRequestDto req
    ) {
        reviewService.create(userId, eventId, req);
    }

    @GetMapping("/api/events/{eventId}/reviews")
    @Operation(summary = "특정 이벤트의 리뷰 목록")
    public Page<EventReviewResponseDto> getEventReviews(
            @CurrentUser UUID userId,
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "LATEST") ReviewSort sort
    ) {
        Pageable pageable = PageRequest.of(page, size, switch (sort) {
            case POPULAR -> Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("id"));
            case LATEST -> Sort.by(Sort.Order.desc("id"));
        });

        return reviewService.getReviewsByEvent(eventId, userId, pageable);
    }

    @GetMapping("/api/events/{eventId}/photoReviews")
    @Operation(summary = "특정 이벤트의 포토리뷰 목록")
    public ResponseEntity<EventPhotoReviewResponseDto> getEventPhotoReviews(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        EventPhotoReviewResponseDto response = reviewService.getEventPhotoReviews(eventId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/reviews/{reviewId}/{photoId}")
    @Operation(summary = "리뷰 상세 조회")
    public ResponseEntity<EventReviewDetailResponseDto> getReviewDetail(
            @PathVariable Long reviewId,
            @PathVariable Long photoId,
            @CurrentUser UUID userId
    ) {
        EventReviewDetailResponseDto response = reviewService.getEventReviewDetail(reviewId, photoId, userId);
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "리뷰 좋아요 추가")
    @PostMapping("/api/reviews/{reviewId}/like")
    public ReviewLikeResponseDto like(
            @PathVariable Long reviewId,
            @CurrentUser UUID userId
    ) {
        return reviewService.like(reviewId, userId);
    }

    @Operation(summary = "리뷰 좋아요 취소")
    @DeleteMapping("/api/reviews/{reviewId}/like")
    public ReviewLikeResponseDto unlike(
            @PathVariable Long reviewId,
            @CurrentUser UUID userId
    ) {
        return reviewService.unlike(reviewId, userId);
    }

    @Operation(summary = "리뷰 수정",
            description = ReviewApiDocs.UPDATE_DESC)
    @PatchMapping("/api/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateReview(
            @PathVariable Long reviewId,
            @CurrentUser UUID userId,
            @Valid @RequestBody ReviewUpdateRequestDto req
    ) {
        reviewService.update(userId, reviewId, req);
    }

    @Operation(summary = "리뷰 삭제")
    @DeleteMapping("/api/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @PathVariable Long reviewId,
            @CurrentUser UUID userId
    ) {
        reviewService.delete(reviewId, userId);
    }

    @PostMapping("/api/reviews/{reviewId}/report")
    @Operation(summary = "리뷰 신고",
            description = ReviewApiDocs.REPORT_DESC)
    public ReviewReportResponseDto reportReview(
            @PathVariable Long reviewId,
            @CurrentUser UUID userId,
            @RequestBody @Valid ReviewReportRequestDto req
    ) {
        return reviewReportService.reportReview(reviewId, userId, req);
    }
}
