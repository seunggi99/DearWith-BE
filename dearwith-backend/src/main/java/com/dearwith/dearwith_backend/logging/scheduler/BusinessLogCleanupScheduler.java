package com.dearwith.dearwith_backend.logging.scheduler;

import com.dearwith.dearwith_backend.logging.repository.BusinessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessLogCleanupScheduler {

    private final BusinessLogRepository businessLogRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOldLogs() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);

        int deletedCount = businessLogRepository.deleteByCreatedAtBefore(threshold);

        log.info("[BusinessLogCleanup] {}일 이전 로그 {}건 삭제 완료 (threshold={})",
                90, deletedCount, threshold);
    }
}