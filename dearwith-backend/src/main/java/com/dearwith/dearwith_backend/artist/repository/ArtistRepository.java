package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
    // 오늘 생일 아티스트
    @Query("SELECT a FROM Artist a WHERE a.birthDate = :today")
    List<Artist> findArtistsByBirthDate(LocalDate today);

    @Query("SELECT a FROM Artist a WHERE FUNCTION('MONTH', a.birthDate) = :month")
    List<Artist> findArtistsByBirthMonth(int month);

    // 이름으로 검색 (부분 일치)
    Page<Artist> findByNameKrContainingIgnoreCaseOrNameEnContainingIgnoreCase(
            String qKr, String qEn, Pageable pageable);

    // 여러 ID로 조회
    List<Artist> findByIdIn(List<Long> ids);

    // 특정 이벤트에 속한 아티스트들 (EventArtistMapping 조인)
    @Query("SELECT m.artist FROM EventArtistMapping m WHERE m.event.id = :eventId")
    List<Artist> findArtistsByEventId(Long eventId);

    @Query("""
        SELECT a FROM Artist a 
        WHERE LOWER(a.nameKr) LIKE LOWER(CONCAT('%', :query, '%')) 
           OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    Page<Artist> searchByName(@Param("query") String query, Pageable pageable);

}