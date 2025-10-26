package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class EventStatusScheduler {

    private final EventRepository eventRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void updateStatusesAtMidnight() {
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
        eventRepository.bulkMarkInProgress(now);
        eventRepository.bulkMarkEnded(now);
        eventRepository.bulkMarkScheduled(now);
    }
}
