package com.dearwith.dearwith_backend.page.main;

import com.dearwith.dearwith_backend.artist.dto.ArtistInfoDto;
import com.dearwith.dearwith_backend.artist.service.ArtistService;
import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class MainPageController {
    private final MainPageService mainPageService;

    // 메인페이지 데이터 전체 조회
    @Operation(summary = "메인페이지 조회(로그인)")
    @GetMapping
    public MainPageResponseDto getMainPage(@AuthenticationPrincipal CustomUserDetails principal) {
        return mainPageService.getMainPage(principal.getId());
    }
}
