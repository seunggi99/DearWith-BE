package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.enums.UserStatus;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserScheduler {
    private final UserRepository userRepository;

    @Transactional
    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")
    public void autoUnsuspendUsers() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        List<User> targets =
                userRepository.findByUserStatusInAndSuspendedUntilBefore(
                        List.of(UserStatus.SUSPENDED, UserStatus.WRITE_RESTRICTED),
                        today.plusDays(1)
                );

        for (User user : targets) {
            user.unsuspend();
        }

        log.info("[auto-unsuspend-users] date={} count={}", today, targets.size());
    }
}
