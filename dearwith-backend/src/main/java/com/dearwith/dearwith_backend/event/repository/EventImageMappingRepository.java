package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventImageMapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
    void deleteByEvent_Id(Long eventId);
    boolean existsByEvent_IdAndImage_Id(Long eventId, Long imageId);
}
