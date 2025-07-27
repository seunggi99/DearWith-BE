package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventBookmarkRepository extends JpaRepository<EventBookmark, Long> {
    boolean existsByEventIdAndUserId(Long eventId, UUID userId);
    Optional<EventBookmark> findByEventIdAndUserId(Long eventId, UUID userId);
}
