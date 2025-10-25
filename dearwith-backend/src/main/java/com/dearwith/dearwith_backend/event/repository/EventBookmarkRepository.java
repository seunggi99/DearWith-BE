package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventBookmark;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventBookmarkRepository extends JpaRepository<EventBookmark, Long> {
    boolean existsByEventIdAndUserId(Long eventId, UUID userId);
    Optional<EventBookmark> findByEventIdAndUserId(Long eventId, UUID userId);

    @EntityGraph(attributePaths = {
            "event",
            "event.coverImage",
            "event.artists",
            "event.artists.artist",
            "event.artists.artist.profileImage"
    })
    Page<EventBookmark> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "event",
            "event.coverImage",
            "event.artists",
            "event.artists.artist",
            "event.artists.artist.profileImage"
    })
    Page<EventBookmark> findByUserIdAndEvent_Status(UUID userId, EventStatus status, Pageable pageable);
}
