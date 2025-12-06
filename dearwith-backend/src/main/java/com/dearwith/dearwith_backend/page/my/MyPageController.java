package com.dearwith.dearwith_backend.page.my;

import com.dearwith.dearwith_backend.artist.dto.ArtistUnifiedDto;
import com.dearwith.dearwith_backend.artist.service.ArtistUnifiedService;
import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.service.EventBookmarkService;
import com.dearwith.dearwith_backend.event.service.EventQueryService;
import com.dearwith.dearwith_backend.page.my.dto.MyReviewResponseDto;
import com.dearwith.dearwith_backend.review.service.ReviewService;
import com.dearwith.dearwith_backend.user.service.UserNotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService myPageService;
    private final ArtistUnifiedService artistUnifiedService;
    private final UserNotificationSettingService userNotificationSettingService;
    private final EventBookmarkService eventBookmarkService;
    private final EventQueryService eventQueryService;
    private final ReviewService reviewService;

    @Operation(summary = "마이페이지 조회" )
    @GetMapping
    public MyPageResponseDto getMyPage(
            @CurrentUser UUID userId
    ){
        MyPageResponseDto response = myPageService.getMyPage(userId);
        return response;
    }

    @Operation(summary = "북마크힌 이벤트 조회")
    @GetMapping("/events/bookmark")
    public Page<EventInfoDto> getBookmarkedEvents(
            @CurrentUser UUID userId,
            @RequestParam String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return eventBookmarkService.getBookmarkedEvents(userId, state, pageable);
    }

    @Operation(summary = "북마크한 아티스트/그룹 조회")
    @GetMapping("/artists/bookmark")
    public Page<ArtistUnifiedDto> getBookmarkedArtists(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return artistUnifiedService.getBookmarkedArtistsAndGroups(userId, pageable);
    }

    @Operation(summary = "내가 등록한 이벤트")
    @GetMapping("/events")
    public Page<EventInfoDto> getMyEvents(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return eventQueryService.getMyEvents(userId, page, size);
    }

    @Operation(summary = "내가 등록한 아티스트")
    @GetMapping("/artists")
    public Page<ArtistUnifiedDto> getMyArtists(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return artistUnifiedService.getMyArtists(userId, page, size);
    }

    @Operation(summary = "내가 작성한 리뷰")
    @GetMapping("/reviews")
    public Page<MyReviewResponseDto> getMyReviews(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "1") Integer months
    ) {
        return reviewService.getMyReviews(userId, page, size, months);
    }

    @Operation(summary = "이벤트 알림 설정 변경")
    @PatchMapping("/notifications/event")
    public boolean updateEventNotification(
            @CurrentUser UUID userId,
            @RequestBody NotificationToggleRequestDto request
    ) {
        return userNotificationSettingService.updateEventNotification(userId, request.isEnabled());
    }

    @Operation(summary = "서비스 알림 설정 변경")
    @PatchMapping("/notifications/service")
    public boolean updateServiceNotification(
            @CurrentUser UUID userId,
            @RequestBody NotificationToggleRequestDto request
    ) {
        return userNotificationSettingService.updateServiceNotification(userId, request.isEnabled());
    }
}
