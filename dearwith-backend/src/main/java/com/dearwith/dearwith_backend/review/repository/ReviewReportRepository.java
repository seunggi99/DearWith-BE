package com.dearwith.dearwith_backend.review.repository;

import com.dearwith.dearwith_backend.review.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
}
