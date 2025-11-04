package com.dearwith.dearwith_backend.review.controller;

import com.dearwith.dearwith_backend.review.dto.EventPhotoReviewResponseDto;
import com.dearwith.dearwith_backend.review.dto.ReviewCreateRequestDto;
import com.dearwith.dearwith_backend.review.dto.EventReviewResponseDto;
import com.dearwith.dearwith_backend.review.enums.ReviewSort;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ReviewController {
    private final ReviewService reviewService;
    @PostMapping("/api/events/{eventId}/reviews")
    @Operation(summary = "이벤트 리뷰 작성")
    public ResponseEntity<Void> create(
            @PathVariable Long eventId,
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @Valid @RequestBody ReviewCreateRequestDto req
    ) {
        reviewService.createReview(userId, eventId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/events/{eventId}/reviews")
    @Operation(summary = "특정 이벤트의 리뷰 목록")
    public Page<EventReviewResponseDto> getEventReviews(
            @AuthenticationPrincipal(expression = "id") UUID userId,
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

    @Operation(summary = "리뷰 좋아요 추가")
    @PostMapping("/api/reviews/{reviewId}/like")
    public ResponseEntity<Void> like(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "id") UUID userId
    ) {
        reviewService.like(reviewId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "리뷰 좋아요 취소")
    @DeleteMapping("/api/reviews/{reviewId}/like")
    public ResponseEntity<Void> unlike(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "id") UUID userId
    ) {
        reviewService.unlike(reviewId, userId);
        return ResponseEntity.ok().build();
    }
}
