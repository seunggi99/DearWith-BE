package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventNoticeRepository extends JpaRepository<EventNotice, Long> {
    Page<EventNotice> findByEventId(Long eventId, Pageable pageable);

    @Modifying
    @Query("delete from EventNotice n where n.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("update EventNotice n set n.viewCount = n.viewCount + 1 where n.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
