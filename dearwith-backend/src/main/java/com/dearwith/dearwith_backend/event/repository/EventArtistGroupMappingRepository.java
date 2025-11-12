package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventArtistGroupMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EventArtistGroupMappingRepository extends JpaRepository<EventArtistGroupMapping, Long> {
    @Query("""
        select eagm
        from EventArtistGroupMapping eagm
        join fetch eagm.artistGroup g
        where eagm.event.id = :eventId
        """)
    List<EventArtistGroupMapping> findByEventId(@Param("eventId") Long eventId);

    /**
     * 이벤트별 그룹 이름 (한 번에 배치 조회)
     */
    interface EventGroupNamesRow {
        Long getEventId();
        String getNameKr();
        String getNameEn();
    }

    @Query("""
        select 
            eag.event.id as eventId,
            g.nameKr      as nameKr,
            g.nameEn      as nameEn
        from EventArtistGroupMapping eag
            join eag.artistGroup g
        where eag.event.id in :eventIds
        """)
    List<EventGroupNamesRow> findGroupNamesByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
