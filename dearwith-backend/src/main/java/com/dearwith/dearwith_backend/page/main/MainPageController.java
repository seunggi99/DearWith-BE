package com.dearwith.dearwith_backend.page.main;

import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.banner.BannerDto;
import com.dearwith.dearwith_backend.banner.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class MainPageController {
    private final MainPageService mainPageService;
    private final BannerService bannerService;

    @Operation(summary = "메인페이지 조회")
    @GetMapping
    public MainPageResponseDto getMainPage(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        MainPageResponseDto response = mainPageService.getMainPage(principal.getId());
        List<BannerDto> banners = bannerService.getMainBanners();
        response.setBanners(banners);

        return response;
    }
}
