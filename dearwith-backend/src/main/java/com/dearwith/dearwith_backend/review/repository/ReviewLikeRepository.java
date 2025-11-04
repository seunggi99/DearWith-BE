package com.dearwith.dearwith_backend.review.repository;

import com.dearwith.dearwith_backend.review.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
}
