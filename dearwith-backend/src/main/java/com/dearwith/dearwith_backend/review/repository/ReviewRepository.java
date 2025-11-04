package com.dearwith.dearwith_backend.review.repository;

import com.dearwith.dearwith_backend.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
