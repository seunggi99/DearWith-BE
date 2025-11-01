package com.dearwith.dearwith_backend.search.controller;

import com.dearwith.dearwith_backend.search.service.RecentSearchService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final RecentSearchService recentSearchService;
    @PostMapping("/recent/add")
    @Operation(summary = "최근 검색어 추가")
    public void add(@AuthenticationPrincipal(expression = "id") UUID userId,
                    @RequestBody String query) {
        recentSearchService.add(userId, query);
    }

    @GetMapping("/recent")
    @Operation(summary = "최근 검색어 목록 조회")
    public List<String> list(@AuthenticationPrincipal(expression = "id") UUID userId) {
        return recentSearchService.list(userId);
    }

    @DeleteMapping("/recent/delete")
    @Operation(summary = "특정 최근 검색어 삭제")
    public void remove(@AuthenticationPrincipal(expression = "id") UUID userId,
                       @RequestParam String query) {
        recentSearchService.remove(userId, query);
    }

    @DeleteMapping("/recent/delete/all")
    @Operation(summary = "전체 최근 검색어 삭제")
    public void clear(@AuthenticationPrincipal(expression = "id") UUID userId) {
        recentSearchService.clear(userId);
    }
}
