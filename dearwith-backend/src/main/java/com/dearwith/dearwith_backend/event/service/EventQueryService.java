package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.dto.EventResponseDto;
import com.dearwith.dearwith_backend.event.dto.EventNoticeResponseDto;
import com.dearwith.dearwith_backend.event.entity.*;
import com.dearwith.dearwith_backend.event.mapper.EventMapper;
import com.dearwith.dearwith_backend.event.repository.*;
import com.dearwith.dearwith_backend.image.entity.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

    private String toImageUrl(Image img) {
        if (img == null) return null;
        if (img.getImageUrl() != null && !img.getImageUrl().isBlank()) return img.getImageUrl();
        if (img.getS3Key() != null && !img.getS3Key().isBlank()) {
            return "https://dearwith-prod-assets-apne2.s3.ap-northeast-2.amazonaws.com/" + img.getS3Key();
        }
        return null;
    }

    // 메인 / 추천 / 핫 / 신규 이벤트들
    @Transactional(readOnly = true)
    public List<EventInfoDto> getRecommendedEvents(UUID userId) {
        return eventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> EventInfoDto.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl(toImageUrl(event.getCoverImage()))
                        .artistNamesKr(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameKr())
                                        .collect(Collectors.toList())
                        )
                        .artistNamesEn(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameEn())
                                        .collect(Collectors.toList())
                        )
                        .startDate(event.getStartDate())
                        .endDate(event.getEndDate())
                        .openTime(event.getOpenTime())
                        .closeTime(event.getCloseTime())
                        .bookmarkCount(event.getBookmarkCount())
                        .bookmarked(
                                userId != null &&
                                        eventBookmarkRepository.existsByEventIdAndUserId(event.getId(), userId)
                        )
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventInfoDto> getHotEvents(UUID userId) {
        // 지금은 추천/신규랑 같은 로직이지만, 나중에 진짜 "핫" 로직으로 교체 가능
        return eventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> EventInfoDto.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl(toImageUrl(event.getCoverImage()))
                        .artistNamesKr(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameKr())
                                        .collect(Collectors.toList())
                        )
                        .artistNamesEn(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameEn())
                                        .collect(Collectors.toList())
                        )
                        .startDate(event.getStartDate())
                        .endDate(event.getEndDate())
                        .openTime(event.getOpenTime())
                        .closeTime(event.getCloseTime())
                        .bookmarkCount(event.getBookmarkCount())
                        .bookmarked(
                                userId != null &&
                                        eventBookmarkRepository.existsByEventIdAndUserId(event.getId(), userId)
                        )
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventInfoDto> getNewEvents(UUID userId) {
        return eventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> EventInfoDto.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl(toImageUrl(event.getCoverImage()))
                        .artistNamesKr(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameKr())
                                        .collect(Collectors.toList())
                        )
                        .artistNamesEn(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameEn())
                                        .collect(Collectors.toList())
                        )
                        .startDate(event.getStartDate())
                        .endDate(event.getEndDate())
                        .openTime(event.getOpenTime())
                        .closeTime(event.getCloseTime())
                        .bookmarkCount(event.getBookmarkCount())
                        .bookmarked(
                                userId != null &&
                                        eventBookmarkRepository.existsByEventIdAndUserId(event.getId(), userId)
                        )
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponseDto getEvent(Long eventId, UUID userId) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found"));

        List<EventImageMapping> mappings =
                mappingRepository.findByEvent_IdOrderByDisplayOrderAsc(eventId);

        List<EventBenefit> benefits =
                benefitRepository.findByEvent_IdOrderByDayIndexAscDisplayOrderAsc(eventId);

        List<EventArtistMapping> artists =
                eventArtistMappingRepository.findByEventId(eventId);

        List<EventArtistGroupMapping> artistGroups =
                eventArtistGroupMappingRepository.findByEventId(eventId);

        List<EventNoticeResponseDto> notices =
                eventNoticeService.getLatestNoticesForEvent(eventId);

        boolean bookmarked = isBookmarked(eventId, userId);

        return mapper.toResponse(e, mappings, benefits, artists, artistGroups, notices, bookmarked);
    }

    @Transactional(readOnly = true)
    public Page<EventInfoDto> search(UUID userId, String query, Pageable pageable) {
        Page<Event> page = eventRepository.searchByTitle(query, pageable);
        Set<Long> bookmarked = bookmarkedIds(userId, page);
        return page.map(event -> EventInfoDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .imageUrl(event.getCoverImage() != null ? event.getCoverImage().getImageUrl() : null)
                .artistNamesKr(event.getArtists().stream()
                        .map(m -> m.getArtist().getNameKr())
                        .filter(Objects::nonNull)
                        .toList())
                .artistNamesEn(event.getArtists().stream()
                        .map(m -> m.getArtist().getNameEn())
                        .filter(Objects::nonNull)
                        .toList())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .openTime(event.getOpenTime())
                .closeTime(event.getCloseTime())
                .bookmarkCount(event.getBookmarkCount())
                .bookmarked(userId == null ? null : bookmarked.contains(event.getId()))
                .build());
    }

    @Transactional(readOnly = true)
    public Page<EventInfoDto> getEventsByArtist(Long artistId, UUID userId, Pageable pageable) {

        if (!artistRepository.existsById(artistId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 1) 이벤트 페이지 조회
        Page<Event> page = eventRepository.findPageByArtistId(artistId, pageable);

        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 2) 배치로 이벤트ID 수집
        List<Long> eventIds = page.getContent().stream().map(Event::getId).toList();

        // 3) 배치로 이벤트별 모든 아티스트 이름 조회
        List<EventArtistMappingRepository.EventArtistNamesRow> names =
                eventArtistMappingRepository.findArtistNamesByEventIds(eventIds);
        Map<Long, List<String>> namesEnMap = new HashMap<>();
        Map<Long, List<String>> namesKrMap = new HashMap<>();
        for (EventArtistMappingRepository.EventArtistNamesRow row : names) {
            namesEnMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>()).add(row.getNameEn());
            namesKrMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>()).add(row.getNameKr());
        }

        // 그룹 이름 배치 조회
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

        // 4) (로그인 시) 북마크 여부 일괄 조회
        Set<Long> bookmarked = bookmarkedIds(userId, eventIds);

        // 5) DTO 매핑
        List<EventInfoDto> dtoList = page.getContent().stream().map(e ->
                EventInfoDto.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .imageUrl(e.getCoverImage() != null ? e.getCoverImage().getImageUrl() : null)
                        .artistNamesEn(namesEnMap.getOrDefault(e.getId(), List.of()))
                        .artistNamesKr(namesKrMap.getOrDefault(e.getId(), List.of()))
                        .groupNamesEn(groupNamesEnMap.getOrDefault(e.getId(), List.of()))
                        .groupNamesKr(groupNamesKrMap.getOrDefault(e.getId(), List.of()))
                        .startDate(e.getStartDate())
                        .endDate(e.getEndDate())
                        .startDate(e.getStartDate())
                        .closeTime(e.getCloseTime())
                        .bookmarkCount(e.getBookmarkCount())
                        .bookmarked(userId == null ? null : bookmarked.contains(e.getId()))
                        .build()
        ).toList();

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<EventInfoDto> getEventsByGroup(Long groupId, UUID userId, Pageable pageable) {

        // 0) 그룹 존재 확인
        if (!artistGroupRepository.existsById(groupId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "아티스트 그룹을 찾을 수 없습니다.");
        }

        // 1) 이벤트 페이지 조회 (그룹 직속 + 소속 아티스트 이벤트 포함)
        Page<Event> page = eventRepository.findPageByGroupOrItsArtists(groupId, pageable);

        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 2) 배치로 이벤트ID 수집
        List<Long> eventIds = page.getContent().stream()
                .map(Event::getId)
                .toList();

        // 3-1) 아티스트 이름 배치 조회
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

        // 3-2) 그룹 이름 배치 조회
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

        // (선택) 중복 제거
        artistNamesEnMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());
        artistNamesKrMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());
        groupNamesEnMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());
        groupNamesKrMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());

        // 4) (로그인 시) 북마크 여부 일괄 조회
        Set<Long> bookmarked = bookmarkedIds(userId, eventIds);

        // 5) DTO 매핑
        List<EventInfoDto> dtoList = page.getContent().stream()
                .map(e -> EventInfoDto.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .imageUrl(e.getCoverImage() != null ? e.getCoverImage().getImageUrl() : null)
                        .artistNamesEn(artistNamesEnMap.getOrDefault(e.getId(), List.of()))
                        .artistNamesKr(artistNamesKrMap.getOrDefault(e.getId(), List.of()))
                        .groupNamesEn(groupNamesEnMap.getOrDefault(e.getId(), List.of()))
                        .groupNamesKr(groupNamesKrMap.getOrDefault(e.getId(), List.of()))
                        .startDate(e.getStartDate())
                        .endDate(e.getEndDate())
                        .closeTime(e.getCloseTime())
                        .bookmarkCount(e.getBookmarkCount())
                        .bookmarked(userId == null ? null : bookmarked.contains(e.getId()))
                        .build()
                )
                .toList();

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
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
}