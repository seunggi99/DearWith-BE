package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.ArtistBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtistBookmarkRepository extends JpaRepository<ArtistBookmark, Long> {
    Optional<ArtistBookmark> findByArtistIdAndUserId(Long artistGroupId, UUID userId);

    Page<ArtistBookmark> findByUserId(UUID userId, Pageable pageable);
    List<ArtistBookmark> findByUserId(UUID userId);
}
