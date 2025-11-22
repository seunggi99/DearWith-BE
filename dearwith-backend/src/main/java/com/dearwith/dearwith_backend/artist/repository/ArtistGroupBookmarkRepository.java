package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.ArtistGroupBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtistGroupBookmarkRepository extends JpaRepository<ArtistGroupBookmark, Long> {
    Optional<ArtistGroupBookmark> findByArtistGroupIdAndUserId(Long artistGroupId, UUID userId);

    List<ArtistGroupBookmark> findByUserId(UUID userId);

    @Query("""
        select agb.artistGroup.id
        from ArtistGroupBookmark agb
        where agb.user.id = :userId
    """)
    List<Long> findGroupIdsByUserId(@Param("userId") UUID userId);

}
