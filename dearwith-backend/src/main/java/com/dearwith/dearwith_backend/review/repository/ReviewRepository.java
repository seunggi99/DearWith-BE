package com.dearwith.dearwith_backend.review.repository;

import com.dearwith.dearwith_backend.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("select r.id from Review r " +
            "where r.event.id = :eventId and r.status = com.dearwith.dearwith_backend.review.enums.ReviewStatus.VISIBLE")
    Page<Long> findIdsByEvent(@Param("eventId") Long eventId, Pageable pageable);

    @Query("""
        select distinct r from Review r
        join fetch r.user u
        left join fetch r.images ri
        left join fetch ri.image img
        where r.id in :ids
        """)
    List<Review> findWithUserAndImagesByIdIn(@Param("ids") Collection<Long> ids);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Review r set r.likeCount = r.likeCount + 1 where r.id = :reviewId")
    int incrementLike(@Param("reviewId") Long reviewId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Review r set r.likeCount = case when r.likeCount > 0 then r.likeCount - 1 else 0 end where r.id = :reviewId")
    int decrementLike(@Param("reviewId") Long reviewId);
}
