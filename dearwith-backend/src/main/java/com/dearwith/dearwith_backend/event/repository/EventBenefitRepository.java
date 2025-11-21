package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventBenefitRepository extends JpaRepository<EventBenefit, Long> {
    List<EventBenefit> findByEvent_IdOrderByDayIndexAscDisplayOrderAsc(Long eventId);

    @Modifying
    @Query("delete from EventBenefit eb where eb.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}
