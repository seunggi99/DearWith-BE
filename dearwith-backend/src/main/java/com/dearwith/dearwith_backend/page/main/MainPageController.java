package com.dearwith.dearwith_backend.page.main;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.banner.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class MainPageController {
    private final MainPageService mainPageService;
    private final BannerService bannerService;

    @Operation(summary = "메인페이지 조회")
    @GetMapping
    public MainPageResponseDto getMainPage(
            @CurrentUser UUID userId
    ) {
        MainPageResponseDto response;
        response = mainPageService.getMainPage(userId);
        response.setBanners(bannerService.getMainBanners());

        return response;
    }
}
