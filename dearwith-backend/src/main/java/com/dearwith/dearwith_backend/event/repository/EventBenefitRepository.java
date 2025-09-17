package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.EventBenefit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventBenefitRepository extends JpaRepository<EventBenefit, Long> {
    List<EventBenefit> findByEvent_IdOrderByDayIndexAscDisplayOrderAsc(Long eventId);
}
