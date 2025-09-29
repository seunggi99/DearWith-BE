package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("SELECT e FROM Event e ORDER BY e.startDate DESC")
    List<Event> findTop10ByOrderByCreatedAtDesc();

    List<Event> findByStatus(EventStatus status);
    List<Event> findByUser_Id(UUID userId);

    @Query("""
    SELECT e FROM Event e
    WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%'))
""")
    Page<Event> searchByTitle(@Param("query") String query, Pageable pageable);

}
