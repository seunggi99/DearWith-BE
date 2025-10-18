package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.ArtistGroupMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistGroupMappingRepository extends JpaRepository<ArtistGroupMapping, Long> {
    boolean existsByArtistIdAndGroupId(Long artistId, Long groupId);
}
