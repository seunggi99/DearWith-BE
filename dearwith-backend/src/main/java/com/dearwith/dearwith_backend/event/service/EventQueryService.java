package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.assembler.EventInfoAssembler;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.dto.EventResponseDto;
import com.dearwith.dearwith_backend.event.dto.EventNoticeResponseDto;
import com.dearwith.dearwith_backend.event.entity.*;
import com.dearwith.dearwith_backend.event.mapper.EventMapper;
import com.dearwith.dearwith_backend.event.repository.*;
import com.dearwith.dearwith_backend.image.asset.ImageVariantAssembler;
import com.dearwith.dearwith_backend.image.asset.ImageVariantProfile;
import com.dearwith.dearwith_backend.image.entity.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EventQueryService {

    private final EventRepository eventRepository;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final EventArtistMappingRepository eventArtistMappingRepository;
    private final EventArtistGroupMappingRepository eventArtistGroupMappingRepository;
    private final EventImageMappingRepository mappingRepository;
    private final EventBenefitRepository benefitRepository;
    private final EventNoticeService eventNoticeService;
    private final EventMapper mapper;
    private final ArtistRepository artistRepository;
    private final ArtistGroupRepository artistGroupRepository;
    private final EventInfoAssembler eventInfoAssembler;
    private final HotEventService hotEventService;
    private final ImageVariantAssembler imageVariantAssembler;

    // 메인페이지(추천/핫/신규)
    @Transactional(readOnly = true)
    public List<EventInfoDto> getRecommendedEvents(UUID userId) {
        List<Event> events = eventRepository.findTop10ByOrderByCreatedAtDesc();
        return buildEventInfoList(events, userId);
    }

    @Transactional(readOnly = true)
    public List<EventInfoDto> getHotEvents(UUID userId) {
        List<EventInfoDto> hotEvents = hotEventService.getHotEvents(10);

        List<Long> ids = hotEvents.stream()
                .map(EventInfoDto::getId)
                .toList();

        List<Event> events = eventRepository.findAllById(ids);

        return buildEventInfoList(events, userId);
    }

    @Transactional(readOnly = true)
    public List<EventInfoDto> getNewEvents(UUID userId) {
        List<Event> events = eventRepository.findTop10ByOrderByCreatedAtDesc();
        return buildEventInfoList(events, userId);
    }

    @Transactional(readOnly = true)
    public EventResponseDto getEvent(Long eventId, UUID userId) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이벤트를 찾을 수 없습니다. id=" + eventId));

        List<EventImageMapping> mappings =
                mappingRepository.findByEvent_IdOrderByDisplayOrderAsc(eventId);

        List<ImageGroupDto> images = toEventImageGroups(mappings);

        List<EventBenefit> benefits =
                benefitRepository.findByEvent_IdOrderByDayIndexAscDisplayOrderAsc(eventId);

        List<EventArtistMapping> artists =
                eventArtistMappingRepository.findByEventId(eventId);

        List<EventArtistGroupMapping> artistGroups =
                eventArtistGroupMappingRepository.findByEventId(eventId);

        List<EventNoticeResponseDto> notices =
                eventNoticeService.getLatestNoticesForEvent(eventId);

        boolean bookmarked = isBookmarked(eventId, userId);

        hotEventService.onEventViewed(eventId, userId);

        return mapper.toResponse(e, images, benefits, artists, artistGroups, notices, bookmarked);
    }

    @Transactional(readOnly = true)
    public Page<EventInfoDto> search(UUID userId, String query, Pageable pageable) {
        Page<Event> page = eventRepository.searchByTitle(query, pageable);

        Set<Long> bookmarked = bookmarkedIds(userId, page);

        return page.map(event ->
                eventInfoAssembler.assemble(
                        event,
                        userId == null ? null : bookmarked.contains(event.getId())
                )
        );
    }
    @Transactional(readOnly = true)
    public Page<EventInfoDto> getEventsByArtist(Long artistId, UUID userId, Pageable pageable) {
        if (!artistRepository.existsById(artistId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "아티스트를 찾을 수 없습니다.");
        }

        Page<Event> page = eventRepository.findPageByArtistId(artistId, pageable);
        return buildEventInfoPageWithBatch(page, userId);
    }

    @Transactional(readOnly = true)
    public Page<EventInfoDto> getEventsByGroup(Long groupId, UUID userId, Pageable pageable) {
        if (!artistGroupRepository.existsById(groupId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "아티스트 그룹을 찾을 수 없습니다.");
        }

        Page<Event> page = eventRepository.findPageByGroupOrItsArtists(groupId, pageable);
        return buildEventInfoPageWithBatch(page, userId);
    }

    /**
     * 공통: Top N, 비페이징 이벤트 목록을 EventInfoDto 리스트로 변환한다.
     */
    private List<EventInfoDto> buildEventInfoList(List<Event> events, UUID userId) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        // 1) 이벤트 ID 모으기
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        // 2) 북마크 일괄 조회
        Set<Long> bookmarked = bookmarkedIds(userId, eventIds);

        // 3) assembler로 DTO 변환 + bookmarked 주입
        return events.stream()
                .map(e -> eventInfoAssembler.assemble(
                        e,
                        userId == null ? null : bookmarked.contains(e.getId())
                ))
                .toList();
    }

    /**
     * 공통: 페이징 + 대량 이벤트 목록을 Batch 방식으로 EventInfoDto 페이지로 변환한다.
     */
    private Page<EventInfoDto> buildEventInfoPageWithBatch(
            Page<Event> page,
            UUID userId
    ) {
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), page.getPageable(), 0);
        }

        // 1) 이벤트 ID 수집
        List<Long> eventIds = page.getContent().stream()
                .map(Event::getId)
                .toList();

        // 2) 아티스트 이름 배치 조회
        List<EventArtistMappingRepository.EventArtistNamesRow> artistNames =
                eventArtistMappingRepository.findArtistNamesByEventIds(eventIds);

        Map<Long, List<String>> artistNamesEnMap = new HashMap<>();
        Map<Long, List<String>> artistNamesKrMap = new HashMap<>();
        for (EventArtistMappingRepository.EventArtistNamesRow row : artistNames) {
            artistNamesEnMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>())
                    .add(row.getNameEn());
            artistNamesKrMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>())
                    .add(row.getNameKr());
        }

        // 3) 그룹 이름 배치 조회
        List<EventArtistGroupMappingRepository.EventGroupNamesRow> groupNames =
                eventArtistGroupMappingRepository.findGroupNamesByEventIds(eventIds);

        Map<Long, List<String>> groupNamesEnMap = new HashMap<>();
        Map<Long, List<String>> groupNamesKrMap = new HashMap<>();
        for (EventArtistGroupMappingRepository.EventGroupNamesRow row : groupNames) {
            groupNamesEnMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>())
                    .add(row.getNameEn());
            groupNamesKrMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>())
                    .add(row.getNameKr());
        }

        artistNamesEnMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());
        artistNamesKrMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());
        groupNamesEnMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());
        groupNamesKrMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());

        // 4) 북마크 일괄 조회
        Set<Long> bookmarked = bookmarkedIds(userId, eventIds);

        // 5) DTO 매핑
        List<EventInfoDto> dtoList = page.getContent().stream()
                .map(e -> eventInfoAssembler.assembleWithBatch(
                        e,
                        userId,
                        artistNamesEnMap,
                        artistNamesKrMap,
                        groupNamesEnMap,
                        groupNamesKrMap,
                        bookmarked
                ))
                .toList();

        return new PageImpl<>(dtoList, page.getPageable(), page.getTotalElements());
    }

    /**
     * 목록 API에서 북마크 여부를 한 번에 조회하기 위한 배치 메서드.
     */
    private Set<Long> bookmarkedIds(UUID userId, Collection<Long> eventIds) {
        if (userId == null || eventIds == null || eventIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> ids = eventBookmarkRepository.findBookmarkedEventIds(userId, eventIds);
        return new HashSet<>(ids);
    }

    /**
     * Page<Event> 바로 넣어 쓰는 편의 오버로드.
     */
    private Set<Long> bookmarkedIds(UUID userId, Page<Event> page) {
        if (page == null || page.isEmpty()) return Collections.emptySet();
        List<Long> eventIds = page.getContent().stream().map(Event::getId).toList();
        return bookmarkedIds(userId, eventIds);
    }

    /**
     * 단건 상세용: exists 체크
     */
    private boolean isBookmarked(Long eventId, UUID userId) {
        if (userId == null) {
            return false;
        }
        return eventBookmarkRepository.existsByEventIdAndUserId(eventId, userId);
    }

    private List<ImageGroupDto> toEventImageGroups(List<EventImageMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return List.of();
        }

        return mappings.stream()
                .map(m -> {
                    Image img = m.getImage();
                    if (img == null || img.getImageUrl() == null) return null;

                    return ImageGroupDto.builder()
                            .id(img.getId())
                            .variants(
                                    imageVariantAssembler.toVariants(
                                            img.getImageUrl(),
                                            ImageVariantProfile.EVENT_DETAIL
                                    )
                            )
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }
}