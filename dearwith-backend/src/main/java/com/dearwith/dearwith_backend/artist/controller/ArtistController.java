package com.dearwith.dearwith_backend.artist.controller;

import com.dearwith.dearwith_backend.artist.dto.ArtistCreateRequestDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistEventsResponseDto;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.artist.service.ArtistService;
import com.dearwith.dearwith_backend.artist.service.HotArtistService;
import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.common.dto.CreatedResponseDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.enums.EventSort;
import com.dearwith.dearwith_backend.event.service.EventQueryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {
    private final ArtistService artistService;
    private final EventQueryService eventQueryService;
    private final ArtistRepository artistRepository;
    private final HotArtistService hotArtistService;
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
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedResponseDto createArtist(
            @Valid @RequestBody ArtistCreateRequestDto req,
            @CurrentUser UUID userId
    ) {
        return artistService.create(userId, req);
    }

    @GetMapping("/{artistId}/events")
    @Operation(summary = "특정 아티스트의 이벤트 목록")
    public ArtistEventsResponseDto getArtistEvents(
            @CurrentUser UUID userId,
            @PathVariable Long artistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "LATEST") EventSort sort
    ) {
        Pageable pageable = PageRequest.of(page, size, switch (sort) {
            case POPULAR -> Sort.by(Sort.Order.desc("bookmarkCount"), Sort.Order.desc("id"));
            case UPCOMING -> Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("id"));
            case LATEST -> Sort.by(Sort.Order.desc("id"));
        });
        hotArtistService.recordArtistView(artistId, userId);

        Page<EventInfoDto> eventPage = eventQueryService.getEventsByArtist(artistId, userId, pageable);

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));

        return ArtistEventsResponseDto.builder()
                .artistId(artist.getId())
                .artistNameKr(artist.getNameKr())
                .page(eventPage)
                .build();
    }

}

