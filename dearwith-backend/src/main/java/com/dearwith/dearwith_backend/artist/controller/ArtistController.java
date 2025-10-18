package com.dearwith.dearwith_backend.artist.controller;

import com.dearwith.dearwith_backend.artist.dto.ArtistCreateRequestDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistDto;
import com.dearwith.dearwith_backend.artist.service.ArtistService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {
    private final ArtistService artistService;

    @GetMapping
    @Operation(summary = "아티스트 검색")
    public Page<ArtistDto> search(
            @RequestParam(name = "query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("nameKr").ascending().and(Sort.by("nameEn").ascending()));

        return artistService.search(query, pageable);
    }

    @PostMapping
    @Operation(summary = "아티스트 등록",
            description = "그룹 검색 API를 통해 그룹을 선택하고, 아티스트 기본정보와 함께 아티스트를 등록합니다." +
                    "그룹 ID가 Null이고 groupName이 존재하는 경우, 새로운 그룹이 생성되어 매핑됩니다.")
    public ResponseEntity<ArtistDto> createArtist(
            @Valid @RequestBody ArtistCreateRequestDto req,
            @AuthenticationPrincipal(expression = "id") UUID userId
    ) {
        ArtistDto response = artistService.create(userId, req);
        return ResponseEntity
                .created(URI.create("/api/artists/" + response.id()))
                .body(response);
    }

}
