package com.dearwith.dearwith_backend.review.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.review.dto.ReviewReportRequestDto;
import com.dearwith.dearwith_backend.review.dto.ReviewReportResponseDto;
import com.dearwith.dearwith_backend.review.entity.Review;
import com.dearwith.dearwith_backend.review.entity.ReviewReport;
import com.dearwith.dearwith_backend.review.enums.ReviewReportStatus;
import com.dearwith.dearwith_backend.review.enums.ReviewStatus;
import com.dearwith.dearwith_backend.review.repository.ReviewReportRepository;
import com.dearwith.dearwith_backend.review.repository.ReviewRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewReportService {

    private static final int AUTO_HIDE_THRESHOLD = 5;

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final UserReader userReader;

    @Transactional
    public ReviewReportResponseDto reportReview(Long reviewId, UUID userId, ReviewReportRequestDto req) {

        /*----------------------------------------------------
         * 1) 리뷰 조회
         *---------------------------------------------------*/
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "리뷰를 찾을 수 없습니다.",
                        "REVIEW_NOT_FOUND"
                ));

        /*----------------------------------------------------
         * 2) 신고 유저 조회
         *---------------------------------------------------*/
        User user = userReader.getLoginAllowedUser(userId);

        /*----------------------------------------------------
         * 3) 중복 신고 방지
         *---------------------------------------------------*/
        if (reviewReportRepository.existsByReviewIdAndUserId(reviewId, user.getId())) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ALREADY_REPORTED,
                    "이미 신고한 리뷰입니다.",
                    "REVIEW_ALREADY_REPORTED"
            );
        }

        /*----------------------------------------------------
         * 4) 신고 엔티티 생성
         *---------------------------------------------------*/
        ReviewReport report = ReviewReport.builder()
                .review(review)
                .user(user)
                .reason(req.reason())
                .content(req.content())
                .status(ReviewReportStatus.PENDING)
                .build();

        /*----------------------------------------------------
         * 5) 신고 카운트 증가
         *---------------------------------------------------*/
        review.incReport();

        boolean autoHidden = false;

        /*----------------------------------------------------
         * 6) 일정 횟수 이상 신고 시 자동 숨김 처리
         *---------------------------------------------------*/
        if (review.getReportCount() >= AUTO_HIDE_THRESHOLD &&
                review.getStatus() == ReviewStatus.VISIBLE) {

            review.setStatus(ReviewStatus.AUTO_HIDDEN);
            report.setStatus(ReviewReportStatus.AUTO_HIDDEN);
            autoHidden = true;
        }

        reviewReportRepository.save(report);

        return new ReviewReportResponseDto(
                review.getId(),
                review.getReportCount(),
                review.getStatus(),
                autoHidden
        );
    }
}