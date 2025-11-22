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
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewReportService {
    private static final int AUTO_HIDE_THRESHOLD = 5;

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewReportRepository reviewReportRepository;

    @Transactional
    public ReviewReportResponseDto reportReview(Long reviewId, UUID userId, ReviewReportRequestDto req) {

        // 1) 리뷰 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다. id=" + reviewId));

        // 2) 신고 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다. id=" + userId));

        // 3) 같은 유저가 같은 리뷰 중복 신고 방지
        if (reviewReportRepository.existsByReviewIdAndUserId(reviewId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED,
                    "이미 신고한 리뷰입니다. reviewId=" + reviewId + ", userId=" + userId);
        }

        // 4) 신고 엔티티 생성 (기본 status = PENDING)
        ReviewReport report = ReviewReport.builder()
                .review(review)
                .user(user)
                .reason(req.reason())
                .content(req.content())
                .status(ReviewReportStatus.PENDING)
                .build();

        // 5) 리뷰 신고카운트 증가
        review.incReport();

        boolean autoHidden = false;

        // 6) 신고카운트가 임계치 넘으면 자동 숨김 처리
        if (review.getReportCount() >= AUTO_HIDE_THRESHOLD
                && review.getStatus() == ReviewStatus.VISIBLE) {

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
