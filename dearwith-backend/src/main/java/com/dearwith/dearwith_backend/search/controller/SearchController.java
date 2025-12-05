package com.dearwith.dearwith_backend.search.controller;

import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.search.service.RecentSearchService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final RecentSearchService recentSearchService;
    @PostMapping("/recent/add")
    @Operation(summary = "최근 검색어 추가")
    public void add(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestBody @NotBlank(message = "검색어를 입력해주세요.") String query
    ) {
        recentSearchService.add(principal.getId(), query);
    }

    @GetMapping("/recent")
    @Operation(summary = "최근 검색어 목록 조회")
    public List<String> list(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        return recentSearchService.list(principal.getId());
    }

    @DeleteMapping("/recent/delete")
    @Operation(summary = "특정 최근 검색어 삭제")
    public void remove(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam String query
    ) {
        recentSearchService.remove(principal.getId(), query);
    }

    @DeleteMapping("/recent/delete/all")
    @Operation(summary = "전체 최근 검색어 삭제")
    public void clear(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal
    ) {
        recentSearchService.clear(principal.getId());
    }
}
