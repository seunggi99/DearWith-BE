package com.dearwith.dearwith_backend.page.my;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class MyPageResponseDto {

    private Profile profile;
    private Stats stats;
    private Notifications notifications;

    @Data
    @Builder(toBuilder = true)
    public static class Profile {
        private String nickname;
        private String profileImageUrl;
    }

    @Data
    @Builder(toBuilder = true)
    public static class Stats {
        private long eventBookmarkCount;
        private long artistBookmarkCount;
        private long reviewCount;
    }

    @Data
    @Builder(toBuilder = true)
    public static class Notifications {
        private boolean eventNotificationEnabled;
        private boolean serviceNotificationEnabled;
    }
}