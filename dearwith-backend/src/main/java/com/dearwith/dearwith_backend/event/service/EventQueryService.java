package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.artist.repository.ArtistBookmarkRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupBookmarkRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.assembler.EventInfoAssembler;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.dto.EventNoticeInfoDto;
import com.dearwith.dearwith_backend.event.dto.EventResponseDto;
import com.dearwith.dearwith_backend.event.entity.*;
import com.dearwith.dearwith_backend.event.mapper.EventMapper;
import com.dearwith.dearwith_backend.event.repository.*;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import com.dearwith.dearwith_backend.image.asset.ImageVariantAssembler;
import com.dearwith.dearwith_backend.image.asset.ImageVariantProfile;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final EventInfoAssembler eventInfoAssembler;
    private final HotEventService hotEventService;
    private final ImageVariantAssembler imageVariantAssembler;
    private final ArtistBookmarkRepository artistBookmarkRepository;
    private final ArtistGroupBookmarkRepository artistGroupBookmarkRepository;
    private final AssetUrlService assetUrlService;
    private final UserReader userReader;
    private final AuthService authService;

    /*──────────────────────────────────────────────
     | 메인페이지 추천 이벤트
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public List<EventInfoDto> getRecommendedEvents(UUID userId) {

        LocalDate today = LocalDate.now();

        // 비로그인 fallback
        if (userId == null) {
            List<Event> fallback = eventRepository.findGlobalRecommendedFallback(
                    today,
                    PageRequest.of(0, 10)
            );

            // 🆕 ID들만 뽑아서 연관관계까지 한 번에 로딩
            List<Long> ids = fallback.stream()
                    .map(Event::getId)
                    .toList();

            if (ids.isEmpty()) {
                return List.of();
            }

            List<Event> loaded = eventRepository.findWithMainPageRelationsByIdIn(ids);
            List<Event> ordered = sortByIdOrder(loaded, ids);

            return buildEventInfoList(ordered, null);
        }

        UUID viewerId = normalizeUserId(userId);

        List<Long> artistIds = artistBookmarkRepository.findArtistIdsByUserId(viewerId);
        List<Long> groupIds  = artistGroupBookmarkRepository.findGroupIdsByUserId(viewerId);

        // 북마크 없으면 global fallback
        if (artistIds.isEmpty() && groupIds.isEmpty()) {
            List<Event> fallback = eventRepository.findGlobalRecommendedFallback(
                    today,
                    PageRequest.of(0, 10)
            );

            List<Long> ids = fallback.stream()
                    .map(Event::getId)
                    .toList();

            if (ids.isEmpty()) {
                return List.of();
            }

            List<Event> loaded = eventRepository.findWithMainPageRelationsByIdIn(ids);
            List<Event> ordered = sortByIdOrder(loaded, ids);

            return buildEventInfoList(ordered, viewerId);
        }

        if (artistIds.isEmpty()) artistIds = List.of(-1L);
        if (groupIds.isEmpty())  groupIds = List.of(-1L);

        PageRequest limit10 = PageRequest.of(0, 10);

        // 1) 개인화 추천 먼저 가져오기
        List<Event> personalized = new ArrayList<>(
                eventRepository.findRecommendedForUser(
                        artistIds,
                        groupIds,
                        today,
                        limit10
                ).getContent()
        );

        // 2) 부족하면 글로벌 fallback으로 채우기
        if (personalized.size() < 10) {
            List<Long> alreadyIds = personalized.stream()
                    .map(Event::getId)
                    .toList();

            List<Event> fallbackList = eventRepository.findGlobalRecommendedFallback(
                    today,
                    PageRequest.of(0, 30)
            );

            fallbackList.stream()
                    .filter(e -> !alreadyIds.contains(e.getId()))
                    .limit(10 - personalized.size())
                    .forEach(personalized::add);
        }

        if (personalized.isEmpty()) {
            return List.of();
        }

        // 3) 최종 ID 리스트 → 연관관계 한 번에 로딩
        List<Long> ids = personalized.stream()
                .map(Event::getId)
                .toList();

        List<Event> loaded = eventRepository.findWithMainPageRelationsByIdIn(ids);
        List<Event> ordered = sortByIdOrder(loaded, ids);

        return buildEventInfoList(ordered, viewerId);
    }

    /*──────────────────────────────────────────────
     | 핫 이벤트
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public List<EventInfoDto> getHotEvents(UUID userId) {
        UUID viewerId = normalizeUserId(userId);
        List<EventInfoDto> hot = hotEventService.getHotEvents(10);

        List<Long> ids = hot.stream()
                .map(EventInfoDto::getId)
                .toList();

        if (ids.isEmpty()) {
            return List.of();
        }

        List<Event> loaded = eventRepository.findWithMainPageRelationsByIdIn(ids);
        List<Event> ordered = sortByIdOrder(loaded, ids);

        return buildEventInfoList(ordered, viewerId);
    }

    /*──────────────────────────────────────────────
     | 최신 이벤트
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public List<EventInfoDto> getNewEvents(UUID userId) {
        UUID viewerId = normalizeUserId(userId);

        List<Event> events = eventRepository.findTop10ByOrderByCreatedAtDesc();
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .toList();

        List<Event> loaded = eventRepository.findWithMainPageRelationsByIdIn(ids);
        List<Event> ordered = sortByIdOrder(loaded, ids);

        return buildEventInfoList(ordered, viewerId);
    }

    /*──────────────────────────────────────────────
     | 이벤트 상세
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public EventResponseDto getEvent(Long eventId, UUID userId) {
        UUID viewerId = normalizeUserId(userId);

        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "이벤트를 찾을 수 없습니다."
                ));

        List<EventImageMapping> mappings =
                mappingRepository.findByEvent_IdOrderByDisplayOrderAsc(eventId);

        List<ImageGroupDto> images = toEventImageGroups(mappings);

        List<EventBenefit> benefits =
                benefitRepository.findByEvent_IdOrderByDayIndexAscDisplayOrderAsc(eventId);

        List<EventArtistMapping> artists =
                eventArtistMappingRepository.findByEventId(eventId);

        List<EventArtistGroupMapping> artistGroups =
                eventArtistGroupMappingRepository.findByEventId(eventId);

        List<EventNoticeInfoDto> notices =
                eventNoticeService.getLatestNoticesForEvent(eventId);

        boolean bookmarked = isBookmarked(eventId, viewerId);
        boolean editable =  authService.isOwner(e.getUser(), userId);

        hotEventService.onEventViewed(eventId, viewerId);

        return mapper.toResponse(
                e,
                images,
                benefits,
                artists,
                artistGroups,
                notices,
                bookmarked,
                assetUrlService,
                editable
        );
    }

    /*──────────────────────────────────────────────
     | 검색
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public Page<EventInfoDto> search(UUID userId, String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "검색어를 입력해주세요.",
                    "EMPTY_QUERY"
            );
        }
        UUID viewerId = normalizeUserId(userId);

        Page<Event> page = eventRepository.searchByTitle(query, pageable);

        Set<Long> bookmarked = bookmarkedIds(viewerId, page);

        return page.map(event ->
                eventInfoAssembler.assemble(
                        event,
                        viewerId == null ? null : bookmarked.contains(event.getId())
                )
        );
    }

    /*──────────────────────────────────────────────
     | 아티스트별 이벤트
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public Page<EventInfoDto> getEventsByArtist(Long artistId, UUID userId, Pageable pageable) {

        if (!artistRepository.existsById(artistId)) {
            throw BusinessException.withMessage(
                    ErrorCode.NOT_FOUND,
                    "아티스트를 찾을 수 없습니다."
            );
        }
        UUID viewerId = normalizeUserId(userId);

        Page<Event> page = eventRepository.findPageByArtistId(artistId, pageable);

        return buildEventInfoPageWithBatch(page, viewerId);
    }

    /*──────────────────────────────────────────────
     | 그룹별 이벤트
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public Page<EventInfoDto> getEventsByGroup(Long groupId, UUID userId, Pageable pageable) {
        if (!artistGroupRepository.existsById(groupId)) {
            throw BusinessException.withMessage(
                    ErrorCode.NOT_FOUND,
                    "아티스트 그룹을 찾을 수 없습니다."
            );
        }
        UUID viewerId = normalizeUserId(userId);

        Page<Event> page = eventRepository.findPageByGroup(groupId, pageable);

        return buildEventInfoPageWithBatch(page, viewerId);
    }

    /*──────────────────────────────────────────────
     | 내가 등록한 이벤트
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public Page<EventInfoDto> getMyEvents(
            UUID userId,
            int page,
            int size
    ) {
        UUID creatorId = normalizeUserId(userId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Event> eventPage = eventRepository.findByUser_Id(
                creatorId,
                pageable
        );

        return buildEventInfoPageWithBatch(eventPage, creatorId);
    }

    /*──────────────────────────────────────────────
     | 공통 EventInfo 빌더 (List)
     *──────────────────────────────────────────────*/
    private List<EventInfoDto> buildEventInfoList(List<Event> events, UUID userId) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        if (userId == null) {
            return events.stream()
                    .map(e -> eventInfoAssembler.assemble(e, false))
                    .toList();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Set<Long> bookmarked = bookmarkedIds(userId, eventIds);


        return events.stream()
                .map(e -> eventInfoAssembler.assemble(e, bookmarked.contains(e.getId())))
                .toList();
    }

    /*──────────────────────────────────────────────
     | 공통 EventInfo 빌더 (Page)
     *──────────────────────────────────────────────*/
    private Page<EventInfoDto> buildEventInfoPageWithBatch(
            Page<Event> page,
            UUID userId
    ) {
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), page.getPageable(), 0);
        }

        List<Long> eventIds = page.getContent().stream().map(Event::getId).toList();

        List<EventArtistMappingRepository.EventArtistNamesRow> artistNames =
                eventArtistMappingRepository.findArtistNamesByEventIds(eventIds);

        Map<Long, List<String>> artistNamesEnMap = new HashMap<>();
        Map<Long, List<String>> artistNamesKrMap = new HashMap<>();

        for (var row : artistNames) {
            artistNamesEnMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>())
                    .add(row.getNameEn());
            artistNamesKrMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>())
                    .add(row.getNameKr());
        }

        List<EventArtistGroupMappingRepository.EventGroupNamesRow> groupNames =
                eventArtistGroupMappingRepository.findGroupNamesByEventIds(eventIds);

        Map<Long, List<String>> groupNamesEnMap = new HashMap<>();
        Map<Long, List<String>> groupNamesKrMap = new HashMap<>();

        for (var row : groupNames) {
            groupNamesEnMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>())
                    .add(row.getNameEn());
            groupNamesKrMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>())
                    .add(row.getNameKr());
        }

        artistNamesEnMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());
        artistNamesKrMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());
        groupNamesEnMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());
        groupNamesKrMap.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().toList());

        Set<Long> bookmarked = bookmarkedIds(userId, eventIds);

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

    private Set<Long> bookmarkedIds(UUID userId, Collection<Long> eventIds) {
        if (userId == null || eventIds == null || eventIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> ids = eventBookmarkRepository.findBookmarkedEventIds(userId, eventIds);
        return new HashSet<>(ids);
    }

    private Set<Long> bookmarkedIds(UUID userId, Page<Event> page) {
        if (page == null || page.isEmpty()) return Collections.emptySet();
        List<Long> ids = page.getContent().stream().map(Event::getId).toList();
        return bookmarkedIds(userId, ids);
    }

    private boolean isBookmarked(Long eventId, UUID userId) {
        if (userId == null) return false;
        return eventBookmarkRepository.existsByEventIdAndUserId(eventId, userId);
    }

    private List<ImageGroupDto> toEventImageGroups(List<EventImageMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return List.of();
        }

        return mappings.stream()
                .map(m -> {
                    Image img = m.getImage();

                    return ImageGroupDto.builder()
                            .id(img.getId())
                            .variants(
                                    imageVariantAssembler.toVariants(
                                            assetUrlService.generatePublicUrl(img),
                                            ImageVariantProfile.EVENT_DETAIL
                                    )
                            )
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private UUID normalizeUserId(UUID userId) {
        if (userId == null) {
            return null;
        }
        // 정지/탈퇴 차단, 작성제한 허용
        return userReader.getLoginAllowedUser(userId).getId();
    }

    private List<Event> sortByIdOrder(List<Event> loaded, List<Long> idOrder) {
        Map<Long, Event> map = loaded.stream()
                .collect(Collectors.toMap(Event::getId, e -> e));
        return idOrder.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .toList();
    }
}