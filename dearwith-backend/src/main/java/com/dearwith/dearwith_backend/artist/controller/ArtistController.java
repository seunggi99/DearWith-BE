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
