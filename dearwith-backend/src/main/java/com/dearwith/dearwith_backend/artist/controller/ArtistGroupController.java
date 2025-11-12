package com.dearwith.dearwith_backend.artist.controller;

import com.dearwith.dearwith_backend.artist.dto.ArtistGroupDto;
import com.dearwith.dearwith_backend.artist.dto.GroupEventsResponseDto;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.service.ArtistGroupService;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.enums.EventSort;
import com.dearwith.dearwith_backend.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class ArtistGroupController {

    private final ArtistGroupService artistGroupService;
    private final EventService eventService;
    private final ArtistGroupRepository groupRepository;

    @GetMapping
    @Operation(summary = "아티스트 그룹 검색")
    public Page<ArtistGroupDto> search(
            @RequestParam(name = "query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("nameKr").ascending().and(Sort.by("nameEn").ascending()));

        return artistGroupService.search(query, pageable);
    }

    @GetMapping("{groupId}/events")
    @Operation(summary = "특정 그룹의 이벤트 목록",
            description = "해당 그룹의 모든 아티스트 이벤트 + 그룹에 직접 매핑된 이벤트를 합쳐서 반환합니다.")
    public GroupEventsResponseDto getGroupEvents(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "LATEST") EventSort sort
    ) {
        Pageable pageable = PageRequest.of(page, size, switch (sort) {
            case POPULAR -> Sort.by(Sort.Order.desc("bookmarkCount"), Sort.Order.desc("id"));
            case UPCOMING -> Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("id"));
            case LATEST -> Sort.by(Sort.Order.desc("id"));
        });

        Page<EventInfoDto> eventPage = eventService.getEventsByGroup(groupId, userId, pageable);

        ArtistGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        return GroupEventsResponseDto.builder()
                .groupId(group.getId())
                .groupNameKr(group.getNameKr())
                .groupNameEn(group.getNameEn())
                .page(eventPage)
                .build();
    }
}
