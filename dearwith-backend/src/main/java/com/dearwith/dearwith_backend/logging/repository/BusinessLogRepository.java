package com.dearwith.dearwith_backend.logging.repository;

import com.dearwith.dearwith_backend.logging.entity.BusinessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface BusinessLogRepository extends JpaRepository<BusinessLog, Long> {
    @Modifying
    @Query("DELETE FROM BusinessLog b WHERE b.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") Instant threshold);
}