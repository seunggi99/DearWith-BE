package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.ArtistBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtistBookmarkRepository extends JpaRepository<ArtistBookmark, Long> {
    Optional<ArtistBookmark> findByArtistIdAndUserId(Long artistGroupId, UUID userId);

    List<ArtistBookmark> findByUserId(UUID userId);

    @Query("""
        select ab.artist.id
        from ArtistBookmark ab
        where ab.user.id = :userId
    """)
    List<Long> findArtistIdsByUserId(@Param("userId") UUID userId);

    @Query("select count(b) from ArtistBookmark b " +
            "where b.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
}
