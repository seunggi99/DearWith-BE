package com.dearwith.dearwith_backend.review.repository;

import com.dearwith.dearwith_backend.review.entity.ReviewImageMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewImageMappingRepository extends JpaRepository<ReviewImageMapping, Long> {
    @Query("""
        select rim
        from ReviewImageMapping rim
        join fetch rim.image img
        where rim.reviewStatus = com.dearwith.dearwith_backend.review.enums.ReviewStatus.VISIBLE
          and rim.eventId = :eventId
        order by rim.id desc
    """)
    Page<ReviewImageMapping> findVisibleLatestByEvent(@Param("eventId") Long eventId, Pageable pageable);

    @Query("select m from ReviewImageMapping m " +
            "join fetch m.image i " +
            "where m.review.id = :reviewId " +
            "order by m.displayOrder asc, m.id asc")
    List<ReviewImageMapping> findByReviewId(@Param("reviewId") Long reviewId);

    @Modifying
    @Query("delete from ReviewImageMapping m where m.review.id = :reviewId")
    void deleteByReviewId(@Param("reviewId") Long reviewId);

    @Modifying
    @Query("delete from ReviewImageMapping m where m.review.id = :reviewId and m.image.id in :imageIds")
    void deleteByReviewIdAndImageIds(@Param("reviewId") Long reviewId, @Param("imageIds") List<Long> imageIds);

    /**
     * 이미지 참조 수 계산.
     */
    @Query("select count(m) from ReviewImageMapping m where m.image.id = :imageId")
    long countUsages(@Param("imageId") Long imageId);
}