package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventArtistMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventArtistMappingRepository extends JpaRepository<EventArtistMapping, Long> {

    @Query("""
        select eam
        from EventArtistMapping eam
        join fetch eam.artist a
        where eam.event.id = :eventId
        """)
    List<EventArtistMapping> findByEventId(@Param("eventId") Long eventId);

    boolean existsByEvent_IdAndArtist_Id(Long eventId, Long artistId);

    void deleteByEvent_Id(Long eventId);
}
