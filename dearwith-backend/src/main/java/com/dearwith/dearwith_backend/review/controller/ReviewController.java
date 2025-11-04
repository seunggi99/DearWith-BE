package com.dearwith.dearwith_backend.review.controller;

import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.review.dto.ReviewCreateRequestDto;
import com.dearwith.dearwith_backend.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events/{eventId}/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable Long eventId,
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @Valid @RequestBody ReviewCreateRequestDto req
    ) {
        reviewService.createReview(userId, eventId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
