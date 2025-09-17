package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventImageMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventImageMappingRepository extends JpaRepository<EventImageMapping, Long> {
    List<EventImageMapping> findByEvent_IdOrderByDisplayOrderAsc(Long eventId);
    void deleteByEvent_Id(Long eventId);
    boolean existsByEvent_IdAndImage_Id(Long eventId, Long imageId);
}
