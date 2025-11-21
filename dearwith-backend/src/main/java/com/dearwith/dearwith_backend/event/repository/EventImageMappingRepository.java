package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventImageMapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventImageMappingRepository extends JpaRepository<EventImageMapping, Long> {
    @Query("""
      select eim
      from EventImageMapping eim
      join fetch eim.image img
      where eim.event.id = :eventId
      order by eim.displayOrder asc
    """)
    List<EventImageMapping> findByEvent_IdOrderByDisplayOrderAsc(@Param("eventId") Long eventId);

    /**
     * 이벤트에 연결된 모든 이미지 매핑 조회
     * - 스냅샷/삭제용으로 사용
     */
    @Query("""
        select m
        from EventImageMapping m
        where m.event.id = :eventId
        order by m.displayOrder asc, m.id asc
    """)
    List<EventImageMapping> findByEventId(@Param("eventId") Long eventId);

    /**
     * 이벤트에 연결된 모든 이미지 매핑 삭제
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from EventImageMapping m
        where m.event.id = :eventId
    """)
    void deleteByEventId(@Param("eventId") Long eventId);

    /**
     * 특정 Image가 매핑 테이블에서 몇 번 사용되는지 카운트
     * - 0이면 고아 이미지로 보고 soft delete 대상
     */
    @Query("""
        select count(m)
        from EventImageMapping m
        where m.image.id = :imageId
    """)
    long countUsages(@Param("imageId") Long imageId);
}
