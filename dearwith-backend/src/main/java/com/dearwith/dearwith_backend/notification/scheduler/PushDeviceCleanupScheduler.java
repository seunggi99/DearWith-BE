package com.dearwith.dearwith_backend.notification.scheduler;

import com.dearwith.dearwith_backend.notification.repository.PushDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushDeviceCleanupScheduler {

    private final PushDeviceRepository pushDeviceRepository;

    private static final Duration DISABLED_KEEP = Duration.ofDays(90);
    private static final Duration DELETED_KEEP  = Duration.ofDays(90);

    @Transactional
    @Scheduled(cron = "0 30 4 * * *")
    public void cleanupPushDevices() {

        Instant now = Instant.now();

        Instant disabledBefore = now.minus(DISABLED_KEEP);
        Instant deletedBefore  = now.minus(DELETED_KEEP);

        // 1) disabled 90일 지난 것 → soft delete
        int softDeleted = pushDeviceRepository.softDeleteDisabledBefore(
                disabledBefore,
                now
        );

        // 2) soft delete 90일 지난 것 → hard delete
        int hardDeleted = pushDeviceRepository.hardDeleteDeletedBefore(
                deletedBefore
        );

        log.info(
                "[PushDeviceCleanup] done. softDeleted={}, hardDeleted={}",
                softDeleted,
                hardDeleted
        );
    }
}