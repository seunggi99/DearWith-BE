package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventNoticeRepository extends JpaRepository<EventNotice, Long> {
    Page<EventNotice> findByEventId(Long eventId, Pageable pageable);
}
