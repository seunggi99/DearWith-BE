package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtistGroupRepository  extends JpaRepository<ArtistGroup, Long> {

    @Query("""
        select g
        from ArtistGroup g
        where g.debutDate is not null
          and month(g.debutDate) = :month
          and g.deletedAt is null
    """)
    List<ArtistGroup> findGroupsByDebutMonth(@Param("month") int month);

    @Query("""
    SELECT g FROM ArtistGroup g
    WHERE 
        LOWER(g.nameKr) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(g.nameEn) LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY
        CASE
            WHEN LOWER(g.nameKr) = LOWER(:query) THEN 0
            WHEN LOWER(g.nameKr) LIKE LOWER(CONCAT(:query, '%')) THEN 1
            WHEN LOWER(g.nameEn) = LOWER(:query) THEN 2
            WHEN LOWER(g.nameEn) LIKE LOWER(CONCAT(:query, '%')) THEN 3
            ELSE 4
        END,
        g.nameKr ASC,
        g.nameEn ASC
    """)
    Page<ArtistGroup> searchByName(@Param("query") String query, Pageable pageable);

    @Query("""
        SELECT g FROM ArtistGroup g
        WHERE 
            LOWER(g.nameKr) LIKE LOWER(CONCAT('%', :query, '%'))
         OR LOWER(g.nameEn) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    List<ArtistGroup> searchByNameForUnified(@Param("query") String query);

    Optional<ArtistGroup> findByNameKrIgnoreCase(String nameKr);
    Optional<ArtistGroup> findByNameEnIgnoreCase(String nameEn);

    @Modifying
    @Query("update ArtistGroup g set g.bookmarkCount = g.bookmarkCount + 1 where g.id = :groupId")
    void incrementBookmark(@Param("groupId") Long groupId);

    @Modifying
    @Query("""
           update ArtistGroup g
           set g.bookmarkCount = case
                                   when g.bookmarkCount > 0 then g.bookmarkCount - 1
                                   else 0
                                 end
           where g.id = :groupId
           """)
    void decrementBookmark(@Param("groupId") Long groupId);

    @Query("select g.bookmarkCount from ArtistGroup g where g.id = :groupId")
    long getBookmarkCount(@Param("groupId") Long groupId);

    List<ArtistGroup> findByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime createdAt);
    Optional<ArtistGroup> findByNameKr(String nameKr);
    boolean existsByNameKr(String nameKr);
}
