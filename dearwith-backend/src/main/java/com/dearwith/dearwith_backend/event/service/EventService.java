package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.artist.service.ArtistService;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.dto.EventCreateRequestDto;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.dto.EventResponseDto;
import com.dearwith.dearwith_backend.event.entity.*;
import com.dearwith.dearwith_backend.event.enums.BenefitType;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import com.dearwith.dearwith_backend.event.mapper.EventMapper;
import com.dearwith.dearwith_backend.event.repository.*;
import com.dearwith.dearwith_backend.external.x.XVerifyPayload;
import com.dearwith.dearwith_backend.external.x.XVerifyTicketService;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.service.ImageAttachmentService;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;
    private final EventArtistMappingRepository eventArtistMappingRepository;
    private final EventArtistGroupMappingRepository eventArtistGroupMappingRepository;
    private final EventImageMappingRepository mappingRepository;
    private final EventBenefitRepository benefitRepository;
    private final EventMapper mapper;
    private final ArtistService artistService;
    private final XVerifyTicketService xVerifyTicketService;
    private final ImageAttachmentService imageAttachmentService;
    private final ArtistGroupRepository artistGroupRepository;

    private String toImageUrl(Image img) {
        if (img == null) return null;
        if (img.getImageUrl() != null && !img.getImageUrl().isBlank()) return img.getImageUrl();
        if (img.getS3Key() != null && !img.getS3Key().isBlank()) {
            return "https://dearwith-prod-assets-apne2.s3.ap-northeast-2.amazonaws.com/" + img.getS3Key();
        }
        return null;
    }

    public List<EventInfoDto> getRecommendedEvents(UUID userId) {
        return eventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> EventInfoDto.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl( toImageUrl(event.getCoverImage()) )
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

    public List<EventInfoDto> getHotEvents(UUID userId) {
        return eventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> EventInfoDto.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl( toImageUrl(event.getCoverImage()) )
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

    public List<EventInfoDto> getNewEvents(UUID userId) {
        return eventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> EventInfoDto.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl( toImageUrl(event.getCoverImage()) )
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

    @Transactional
    public EventResponseDto create(UUID userId, EventCreateRequestDto req) {
        // 0) Organizer 필수 확인
        if (req.organizer() == null) {
            throw new BusinessException(ErrorCode.ORGANIZER_REQUIRED);
        }

        // 0-1) X 인증 티켓 확인/소모
        XVerifyPayload xPayload = null;
        if (req.organizer().xTicket() != null && !req.organizer().xTicket().isBlank()) {
            try {
                xPayload = xVerifyTicketService.confirmAndConsume(req.organizer().xTicket(), userId);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.X_TICKET_EXPIRED, e.getMessage());
            }
        }

        // 1) Event 매핑 + 날짜 검증
        Event event = mapper.toEvent(userId, req);
        if (event.getStartDate() == null) throw new BusinessException(ErrorCode.EVENT_START_REQUIRED);
        if (event.getEndDate() != null && event.getEndDate().isBefore(event.getStartDate()))
            throw new BusinessException(ErrorCode.EVENT_DATE_RANGE_INVALID);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate start = event.getStartDate();
        LocalDate end   = event.getEndDate();

        EventStatus status;
        if (end == null) {
            status = (today.isBefore(start)) ? EventStatus.SCHEDULED : EventStatus.IN_PROGRESS;
        } else if (today.isBefore(start)) {
            status = EventStatus.SCHEDULED;
        } else if (!today.isAfter(end)) {
            status = EventStatus.IN_PROGRESS;
        } else {
            status = EventStatus.ENDED;
        }
        event.setStatus(status);

        // 1-1) Organizer X 인증 정보
        if (xPayload != null) {
            event.setOrganizer(OrganizerInfo.builder()
                    .xId(xPayload.xId())
                    .xHandle(xPayload.xHandle())
                    .xName(xPayload.xName())
                    .verified(xPayload.verified())
                    .build());
        } else {
            event.setOrganizer(OrganizerInfo.builder()
                    .xHandle(req.organizer().xHandle())
                    .verified(false)
                    .build());
        }

        // 2) 이미지 매핑
        if (req.images() != null && !req.images().isEmpty()) {
            var imageDtos = req.images().stream()
                    .map(d -> new ImageAttachmentRequestDto(d.tmpKey(), d.displayOrder()))
                    .toList();
            imageAttachmentService.setEventImages(event, imageDtos, userId);
        }

        // 3. 특전 매핑
        if (req.benefits() != null) {
            LocalDate eventStart = event.getStartDate();
            if (eventStart == null) {
                throw new IllegalStateException("이벤트 시작일이 설정되지 않았습니다.");
            }

            for (var b : req.benefits()) {
                int displayOrder = (b.displayOrder() != null) ? b.displayOrder() : 0;

                Integer dayIndex = b.dayIndex();
                LocalDate visibleFrom = null;

                if (b.benefitType() == BenefitType.LIMITED) {
                    if (dayIndex == null) {
                        dayIndex = 1;
                    }
                    if (dayIndex < 1) {
                        throw new BusinessException(ErrorCode.BENEFIT_DAYINDEX_INVALID);
                    }
                    visibleFrom = eventStart.plusDays(dayIndex - 1L);
                }

                EventBenefit benefit = EventBenefit.builder()
                        .event(event)
                        .name(b.name())
                        .benefitType(b.benefitType())
                        .dayIndex(dayIndex)
                        .visibleFrom(visibleFrom)
                        .displayOrder(displayOrder)
                        .active(true)
                        .build();

                event.addBenefit(benefit);
            }
        }

        // 4. 아티스트 매핑
        if (req.artistIds() != null && !req.artistIds().isEmpty()) {
            List<Artist> artists = artistService.findAllByIds(req.artistIds());
            // 존재하지 않는 ID 필터링
            Set<Long> foundIds = artists.stream().map(Artist::getId).collect(Collectors.toSet());
            List<Long> missing = req.artistIds().stream().filter(id -> !foundIds.contains(id)).toList();
            if (!missing.isEmpty()) {
                throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND, "존재하지 않는 아티스트 ID: " + missing);
            }

            for (Artist artist : artists) {
                EventArtistMapping mapping = EventArtistMapping.builder()
                        .event(event)
                        .artist(artist)
                        .build();
                event.addArtistMapping(mapping);
            }
        }

        // 4-1) 그룹 매핑
        if (req.artistGroupIds() != null && !req.artistGroupIds().isEmpty()) {
            Set<Long> uniqueGroupIds = new LinkedHashSet<>(req.artistGroupIds());
            for (Long groupId : uniqueGroupIds) {
                ArtistGroup group = artistGroupRepository.findById(groupId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "아티스트 그룹을 찾을 수 없습니다."));

                EventArtistGroupMapping groupMapping = EventArtistGroupMapping.builder()
                        .event(event)
                        .artistGroup(group)
                        .build();

                event.addArtistGroupMapping(groupMapping);
            }
        }

        validateAndNormalize(event);

        // 5. 저장
        Event saved = eventRepository.save(event);

        // 6. 응답 DTO 구성
        List<EventImageMapping> mappings =
                mappingRepository.findByEvent_IdOrderByDisplayOrderAsc(saved.getId());
        List<EventBenefit> benefits =
                benefitRepository.findByEvent_IdOrderByDayIndexAscDisplayOrderAsc(saved.getId());
        List<EventArtistMapping> artists =
                eventArtistMappingRepository.findByEventId(saved.getId());
        List<EventArtistGroupMapping> artistGroups =
                eventArtistGroupMappingRepository.findByEventId(saved.getId());

        return mapper.toResponse(saved, mappings, benefits, artists,artistGroups);
    }

    private void validateAndNormalize(Event event) {
        if (event.getStartDate() != null && event.getEndDate() != null
                && event.getEndDate().isBefore(event.getStartDate())) {
            throw new BusinessException(ErrorCode.EVENT_DATE_RANGE_INVALID);
        }

        if (event.getOrganizer() == null) {
            event.setOrganizer(new OrganizerInfo());
        }
        event.getOrganizer().normalize();

        if (event.getTitle() != null) {
            event.setTitle(event.getTitle().trim());
        }
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

        return mapper.toResponse(e, mappings, benefits, artists, artistGroups, isBookmarked(eventId, userId));
    }

    @Transactional
    public void addBookmark(Long eventId, UUID userId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        try {
            eventBookmarkRepository.save(EventBookmark.builder()
                    .event(event).user(user).build());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_BOOKMARKED_EVENT);
        }

        eventRepository.incrementBookmark(eventId);
    }

    @Transactional
    public void removeBookmark(Long eventId, UUID userId) {
        EventBookmark bookmark = eventBookmarkRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKMARK_NOT_FOUND));

        eventBookmarkRepository.delete(bookmark);
        eventRepository.decrementBookmark(bookmark.getEvent().getId());
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
    public Page<EventInfoDto> getBookmarkedEvents(UUID userId, String state, Pageable pageable) {

        EventStatus statusFilter = null;

        if (state != null) {
            try {
                statusFilter = EventStatus.valueOf(state.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_EVENT_STATUS);
            }
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EventBookmark> bookmarks =
                (statusFilter != null)
                        ? eventBookmarkRepository.findByUserIdAndEvent_Status(userId, statusFilter, sortedPageable)
                        : eventBookmarkRepository.findByUserId(userId, sortedPageable);

        return bookmarks.map(bookmark -> {
            Event event = bookmark.getEvent();
            return EventInfoDto.builder()
                    .id(event.getId())
                    .title(event.getTitle())
                    .imageUrl(
                            event.getCoverImage() != null
                                    ? event.getCoverImage().getImageUrl()
                                    : null
                    )
                    .artistNamesKr(
                            event.getArtists().stream()
                                    .map(m -> m.getArtist().getNameKr())
                                    .filter(Objects::nonNull)
                                    .toList()
                    )
                    .artistNamesEn(
                            event.getArtists().stream()
                                    .map(m -> m.getArtist().getNameEn())
                                    .filter(Objects::nonNull)
                                    .toList()
                    )
                    .startDate(event.getStartDate())
                    .endDate(event.getEndDate())
                    .openTime(event.getOpenTime())
                    .closeTime(event.getCloseTime())
                    .bookmarkCount(event.getBookmarkCount())
                    .bookmarked(true)
                    .build();
        });
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
    public boolean isBookmarked(Long eventId, UUID userId) {
        if (userId == null) {
            return false;
        }
        return eventBookmarkRepository.existsByEventIdAndUserId(eventId, userId);
    }

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
        List<EventArtistMappingRepository.EventArtistNamesRow> names = eventArtistMappingRepository.findArtistNamesByEventIds(eventIds);
        Map<Long, List<String>> namesEnMap = new HashMap<>();
        Map<Long, List<String>> namesKrMap = new HashMap<>();
        for (EventArtistMappingRepository.EventArtistNamesRow row : names) {
            namesEnMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>()).add(row.getNameEn());
            namesKrMap.computeIfAbsent(row.getEventId(), k -> new ArrayList<>()).add(row.getNameKr());
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
                        .groupNamesKr(groupNamesKrMap.getOrDefault(e.getId(), List.of()))                        .startDate(e.getStartDate())
                        .endDate(e.getEndDate())
                        .startDate(e.getStartDate())
                        .closeTime(e.getCloseTime())
                        .bookmarkCount(e.getBookmarkCount())
                        .bookmarked(userId == null ? null : bookmarked.contains(e.getId()))
                        .build()
        ).toList();

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }
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
}
