package com.dearwith.dearwith_backend.notification.event;


import com.dearwith.dearwith_backend.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationEventListener {

    private final PushNotificationService pushNotificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(PushNotificationEvent event) {
        if (event == null || event.userIds() == null || event.userIds().isEmpty()) return;

        try {
            pushNotificationService.sendToUsers(
                    event.userIds(),
                    event.title(),
                    event.body(),
                    event.url()
            );
        } catch (Exception e) {
            log.error("[push-event] failed. size={}, title={}", event.userIds().size(), event.title(), e);
        }
    }
}