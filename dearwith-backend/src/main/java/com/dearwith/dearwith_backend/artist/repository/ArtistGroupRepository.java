package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArtistGroupRepository  extends JpaRepository<ArtistGroup, Long> {
    @Query("""
        SELECT a FROM ArtistGroup a 
        WHERE LOWER(a.nameKr) LIKE LOWER(CONCAT('%', :query, '%')) 
           OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    Page<ArtistGroup> searchByName(@Param("query") String query, Pageable pageable);

    Optional<ArtistGroup> findByNameKrIgnoreCase(String nameKr);
    Optional<ArtistGroup> findByNameEnIgnoreCase(String nameEn);
}
