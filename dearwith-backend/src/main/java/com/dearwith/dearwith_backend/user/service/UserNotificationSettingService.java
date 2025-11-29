package com.dearwith.dearwith_backend.user.service;


import com.dearwith.dearwith_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserNotificationSettingService {

    private final UserReader userReader;

    @Transactional
    public boolean updateEventNotification(UUID userId, boolean enabled) {
        User user = userReader.getLoginAllowedUser(userId);
        user.updateEventNotification(enabled);
        return user.isEventNotificationEnabled();
    }
    @Transactional
    public boolean updateServiceNotification(UUID userId, boolean enabled) {
        User user = userReader.getLoginAllowedUser(userId);
        user.updateServiceNotification(enabled);
        return user.isServiceNotificationEnabled();
    }
}
