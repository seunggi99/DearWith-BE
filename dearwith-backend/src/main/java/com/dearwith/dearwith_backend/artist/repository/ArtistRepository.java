package com.dearwith.dearwith_backend.artist.repository;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    @Query("""
        select a
        from Artist a
        where a.birthDate is not null
          and month(a.birthDate) = :month
          and a.deletedAt is null
    """)
    List<Artist> findArtistsByBirthMonth(@Param("month") int month);

    @Query("""
        select a
        from Artist a
        where a.debutDate is not null
          and month(a.debutDate) = :month
          and a.deletedAt is null
    """)
    List<Artist> findArtistsByDebutMonth(@Param("month") int month);

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

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Artist a
        WHERE (LOWER(a.nameKr) = LOWER(:name)
            OR LOWER(a.nameEn) = LOWER(:name))
          AND a.birthDate = :birthDate
    """)
    boolean existsByNameKrOrEnIgnoreCaseAndBirthDate(@Param("name") String name,
                                                     @Param("birthDate") LocalDate birthDate);


    @Modifying
    @Query("update Artist a set a.bookmarkCount = a.bookmarkCount + 1 where a.id = :artistId")
    void incrementBookmark(@Param("artistId") Long artistId);

    @Modifying
    @Query("""
           update Artist a
           set a.bookmarkCount = case
                                   when a.bookmarkCount > 0 then a.bookmarkCount - 1
                                   else 0
                                 end
           where a.id = :artistId
           """)
    void decrementBookmark(@Param("artistId") Long artistId);

    @Query("select a.bookmarkCount from Artist a where a.id = :artistId")
    long getBookmarkCount(@Param("artistId") Long artistId);

}