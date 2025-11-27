package com.dearwith.dearwith_backend.image.repository;

import com.dearwith.dearwith_backend.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, Long> {
    @Query("SELECT COUNT(a) FROM Artist a WHERE a.profileImage.id = :imageId")
    long countArtistProfileUsages(@Param("imageId") Long imageId);

    @Query("SELECT COUNT(ag) FROM ArtistGroup ag WHERE ag.profileImage.id = :imageId")
    long countArtistGroupProfileUsages(@Param("imageId") Long imageId);
}
