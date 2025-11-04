package com.dearwith.dearwith_backend.review.repository;

import com.dearwith.dearwith_backend.review.entity.ReviewImageMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}