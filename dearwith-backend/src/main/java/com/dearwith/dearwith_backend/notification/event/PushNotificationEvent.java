package com.dearwith.dearwith_backend.notification.event;

import java.util.List;
import java.util.UUID;

public record PushNotificationEvent(
        List<UUID> userIds,
        String title,
        String body,
        String url
) {
}
