package com.dearwith.dearwith_backend.review.repository;

import com.dearwith.dearwith_backend.review.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    boolean existsByReviewIdAndUserId(Long reviewId, UUID userId);
    Optional<ReviewLike> findByReviewIdAndUserId(Long reviewId, UUID userId);

    @Query("""
        select rl.review.id
          from ReviewLike rl
         where rl.user.id = :userId
           and rl.review.id in :reviewIds
    """)
    List<Long> findLikedReviewIds(@Param("userId") UUID userId,
                                  @Param("reviewIds") Collection<Long> reviewIds);
}
