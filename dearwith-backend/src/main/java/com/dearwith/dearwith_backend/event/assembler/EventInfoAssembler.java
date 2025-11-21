package com.dearwith.dearwith_backend.event.assembler;

import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.image.asset.ImageVariantAssembler;
import com.dearwith.dearwith_backend.image.asset.ImageVariantProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class EventInfoAssembler {

    private final ImageVariantAssembler imageVariantAssembler;
    /**
     * 1) 단일 Event → 단순 DTO 변환
     */
    public EventInfoDto assemble(Event e, Boolean bookmarked) {
        return EventInfoDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .images(buildCoverImageGroups(e))
                .artistNamesEn(
                        e.getArtists().stream()
                                .map(m -> m.getArtist().getNameEn())
                                .filter(Objects::nonNull)
                                .toList()
                )
                .artistNamesKr(
                        e.getArtists().stream()
                                .map(m -> m.getArtist().getNameKr())
                                .filter(Objects::nonNull)
                                .toList()
                )
                .groupNamesEn(
                        e.getArtistGroups().stream()
                                .map(m -> m.getArtistGroup().getNameEn())
                                .filter(Objects::nonNull)
                                .toList()
                )
                .groupNamesKr(
                        e.getArtistGroups().stream()
                                .map(m -> m.getArtistGroup().getNameKr())
                                .filter(Objects::nonNull)
                                .toList()
                )
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .closeTime(e.getCloseTime())
                .bookmarkCount(e.getBookmarkCount())
                .bookmarked(bookmarked)
                .build();
    }

    public EventInfoDto assemble(Event e) {
        return assemble(e, null);
    }

    /**
     * 2) 배치 기반 DTO 구성 (아티스트/그룹 이름을 미리 Map 형태로 만든 경우)
     */
    public EventInfoDto assembleWithBatch(
            Event e,
            UUID userId,
            Map<Long, List<String>> artistNamesEnMap,
            Map<Long, List<String>> artistNamesKrMap,
            Map<Long, List<String>> groupNamesEnMap,
            Map<Long, List<String>> groupNamesKrMap,
            Set<Long> bookmarked
    ) {
        Long id = e.getId();

        return EventInfoDto.builder()
                .id(id)
                .title(e.getTitle())
                .images(buildCoverImageGroups(e))
                .artistNamesEn(artistNamesEnMap.getOrDefault(id, List.of()))
                .artistNamesKr(artistNamesKrMap.getOrDefault(id, List.of()))
                .groupNamesEn(groupNamesEnMap.getOrDefault(id, List.of()))
                .groupNamesKr(groupNamesKrMap.getOrDefault(id, List.of()))
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .closeTime(e.getCloseTime())
                .bookmarkCount(e.getBookmarkCount())
                .bookmarked(userId == null ? null : bookmarked.contains(id))
                .build();
    }

    public List<ImageGroupDto> buildCoverImageGroups(Event e) {
        ImageGroupDto dto = toCoverImageGroup(e);
        return (dto == null) ? List.of() : List.of(dto);
    }

    public ImageGroupDto toCoverImageGroup(Event event) {
        var cover = event.getCoverImage();
        if (cover == null) return null;

        String baseUrl = cover.getImageUrl();

        return ImageGroupDto.builder()
                .id(cover.getId())
                .variants(imageVariantAssembler.toVariants(baseUrl, ImageVariantProfile.EVENT_LIST))
                .build();
    }
}

