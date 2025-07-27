package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
    // 오늘 생일 아티스트
    @Query("SELECT a FROM Artist a WHERE a.birthDate = :today")
    List<Artist> findArtistsByBirthDate(LocalDate today);

    @Query("SELECT a FROM Artist a WHERE FUNCTION('MONTH', a.birthDate) = :month")
    List<Artist> findArtistsByBirthMonth(int month);
}