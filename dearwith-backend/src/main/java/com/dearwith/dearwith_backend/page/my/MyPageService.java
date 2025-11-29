package com.dearwith.dearwith_backend.page.my;

import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.event.repository.EventBookmarkRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistBookmarkRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupBookmarkRepository;
import com.dearwith.dearwith_backend.review.repository.ReviewRepository;
import com.dearwith.dearwith_backend.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final EventBookmarkRepository eventBookmarkRepository;
    private final ArtistBookmarkRepository artistBookmarkRepository;
    private final ArtistGroupBookmarkRepository artistGroupBookmarkRepository;
    private final ReviewRepository reviewRepository;
    private final AssetUrlService assetUrlService;
    private final UserReader userReader;

    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPage(UUID userId) {

        User user = userReader.getLoginAllowedUser(userId);

        /* 프로필 */
        String profileImageUrl = null;
        if (user.getProfileImage() != null) {
            profileImageUrl = assetUrlService.generatePublicUrl(user.getProfileImage());
        }

        MyPageResponseDto.Profile profile = MyPageResponseDto.Profile.builder()
                .nickname(user.getNickname())
                .profileImageUrl(profileImageUrl)
                .build();

        /* 통계 */
        long eventBookmarkCount  = eventBookmarkRepository.countByUserId(user.getId());
        long artistBookmarkCount = artistBookmarkRepository.countByUserId(user.getId()) + artistGroupBookmarkRepository.countByUserId(user.getId());
        long reviewCount = reviewRepository.countByUserId(user.getId());

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
}