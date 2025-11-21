package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.ArtistGroupBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtistGroupBookmarkRepository extends JpaRepository<ArtistGroupBookmark, Long> {
    Optional<ArtistGroupBookmark> findByArtistGroupIdAndUserId(Long artistGroupId, UUID userId);

    Page<ArtistGroupBookmark> findByUserId(UUID userId, Pageable pageable);

    List<ArtistGroupBookmark> findByUserId(UUID userId);

}
