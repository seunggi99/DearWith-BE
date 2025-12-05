package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroupMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistGroupMappingRepository extends JpaRepository<ArtistGroupMapping, Long> {
    boolean existsByArtistIdAndGroupId(Long artistId, Long groupId);

    boolean existsByArtistAndGroup(Artist artist, ArtistGroup group);

    List<ArtistGroupMapping> findAllByArtist(Artist artist);

    List<ArtistGroupMapping> findAllByGroup(ArtistGroup group);
}
