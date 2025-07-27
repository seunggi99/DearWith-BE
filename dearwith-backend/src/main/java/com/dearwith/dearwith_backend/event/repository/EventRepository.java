package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("SELECT e FROM Event e ORDER BY e.startDate DESC")
    List<Event> findTop10ByOrderByCreatedAtDesc();
}
