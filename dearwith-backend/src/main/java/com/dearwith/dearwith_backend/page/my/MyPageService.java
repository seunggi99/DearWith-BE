package com.dearwith.dearwith_backend.page.my;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import com.dearwith.dearwith_backend.event.repository.EventBookmarkRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistBookmarkRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupBookmarkRepository;
import com.dearwith.dearwith_backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepository;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final ArtistBookmarkRepository artistBookmarkRepository;
    private final ArtistGroupBookmarkRepository artistGroupBookmarkRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPage(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        /* 프로필 */
        String profileImageUrl = null;
        if (user.getProfileImage() != null) {
            profileImageUrl = user.getProfileImage().getImageUrl();
        }

        MyPageResponseDto.Profile profile = MyPageResponseDto.Profile.builder()
                .nickname(user.getNickname())
                .profileImageUrl(profileImageUrl)
                .build();

        /* 통계 */
        long eventBookmarkCount  = eventBookmarkRepository.countByUserId(userId);
        long artistBookmarkCount = artistBookmarkRepository.countByUserId(userId) + artistGroupBookmarkRepository.countByUserId(userId);
        long reviewCount = reviewRepository.countByUserId(userId);

        MyPageResponseDto.Stats stats = MyPageResponseDto.Stats.builder()
                .eventBookmarkCount(eventBookmarkCount)
                .artistBookmarkCount(artistBookmarkCount)
                .reviewCount(reviewCount)
                .build();

        /* 알림 설정 */
        MyPageResponseDto.Notifications notifications = MyPageResponseDto.Notifications.builder()
                .eventNotificationEnabled(user.isEventNotificationEnabled())
                .serviceNotificationEnabled(user.isServiceNotificationEnabled())
                .build();

        return MyPageResponseDto.builder()
                .profile(profile)
                .stats(stats)
                .notifications(notifications)
                .build();
    }

    @Transactional
    public boolean updateEventNotification(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."
                ));

        user.updateEventNotification(enabled);
        return user.isEventNotificationEnabled();
    }

    @Transactional
    public boolean updateServiceNotification(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."
                ));

        user.updateServiceNotification(enabled);
        return user.isServiceNotificationEnabled();
    }
}