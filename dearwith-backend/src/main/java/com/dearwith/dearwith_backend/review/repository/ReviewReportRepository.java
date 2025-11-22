package com.dearwith.dearwith_backend.review.repository;

import com.dearwith.dearwith_backend.review.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    boolean existsByReviewIdAndUserId(Long reviewId, UUID userId);
}
