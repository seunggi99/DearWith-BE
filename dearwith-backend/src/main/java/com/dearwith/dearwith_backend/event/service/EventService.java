package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.service.ArtistService;
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
import com.dearwith.dearwith_backend.image.Image;
import com.dearwith.dearwith_backend.image.ImageService;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final UserRepository userRepository;
    private final EventImageMappingRepository mappingRepository;
    private final EventBenefitRepository benefitRepository;
    private final EventMapper mapper;
    private final ImageService imageService;
    private final EventArtistMappingRepository artistMappingRepository;
    private final ArtistService artistService;
    private final XVerifyTicketService xVerifyTicketService;

    public List<EventInfoDto> getRecommendedEvents(UUID userId) {
        return eventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> EventInfoDto.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl(event.getCoverImage().getImageUrl())
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
                        .imageUrl(event.getCoverImage().getImageUrl())
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
                        .imageUrl(event.getCoverImage().getImageUrl())
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
                        .bookmarkCount(event.getBookmarkCount())
                        .bookmarked(
                                userId != null &&
                                        eventBookmarkRepository.existsByEventIdAndUserId(event.getId(), userId)
                        )
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public EventResponseDto createEvent(UUID userId, EventCreateRequestDto req) {
        // 0) X 인증 티켓 확인/소모 (있을 때만)
        XVerifyPayload xPayload = null;
        if (req.organizer().xTicket() != null && !req.organizer().xTicket().isBlank()) {
            xPayload = xVerifyTicketService.confirmAndConsume(req.organizer().xTicket(), userId);
        }

        // 1) Event 기본 매핑
        Event event = mapper.toEvent(userId, req);
        if (event.getStatus() == null) {
            event.setStatus(EventStatus.SCHEDULED);
        }

        // 1-1) X 인증 정보 바인딩
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

        // 2. 이미지 매핑 (tmp → inline 커밋)
        Image coverCandidate = null;
        if (req.images() != null && !req.images().isEmpty()) {
            boolean coverSet = (event.getCoverImage() != null);

            for (var dto : req.images()) {
                Image image = imageService.registerCommittedImage(dto.tmpKey(), userId);

                EventImageMapping m = EventImageMapping.builder()
                        .event(event)
                        .image(image)
                        .displayOrder(dto.displayOrder())
                        .build();
                event.addImageMapping(m);

                // 첫 번째 것만 커버로 세팅
                if (!coverSet) {
                    coverCandidate = image;
                    coverSet = true;
                }
            }
        }
        if (coverCandidate != null) {
            event.setCoverImage(coverCandidate);
        }

        // 3. 특전 매핑
        if (req.benefits() != null) {
            LocalDate eventStart = event.getStartDate(); // 이벤트 시작일이 null이면 예외
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
                        throw new IllegalArgumentException("LIMITED 특전의 dayIndex는 1 이상이어야 합니다. 입력값: " + dayIndex);
                    }
                    visibleFrom = eventStart.plusDays(dayIndex - 1L);
                }

                EventBenefit benefit = EventBenefit.builder()
                        .event(event)
                        .name(b.name())
                        .description(b.description())
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
            // 존재하지 않는 ID 필터링 체크(선택)
            Set<Long> foundIds = artists.stream().map(Artist::getId).collect(Collectors.toSet());
            List<Long> missing = req.artistIds().stream().filter(id -> !foundIds.contains(id)).toList();
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("존재하지 않는 아티스트 ID: " + missing);
            }

            for (Artist artist : artists) {
                EventArtistMapping mapping = EventArtistMapping.builder()
                        .event(event)
                        .artist(artist)
                        .build();
                event.addArtistMapping(mapping);
            }
        }

        // 5. 저장
        Event saved = eventRepository.save(event);

        // 6. 응답 DTO 구성
        List<EventImageMapping> mappings =
                mappingRepository.findByEvent_IdOrderByDisplayOrderAsc(saved.getId());
        List<EventBenefit> benefits =
                benefitRepository.findByEvent_IdOrderByDayIndexAscDisplayOrderAsc(saved.getId());
        List<EventArtistMapping> artists =
                artistMappingRepository.findByEventId(saved.getId());

        return mapper.toResponse(saved, mappings, benefits, artists);
    }


    @Transactional(readOnly = true)
    public EventResponseDto getEvent(Long eventId) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found"));

        List<EventImageMapping> mappings =
                mappingRepository.findByEvent_IdOrderByDisplayOrderAsc(eventId);

        List<EventBenefit> benefits =
                benefitRepository.findByEvent_IdOrderByDayIndexAscDisplayOrderAsc(eventId);

        List<EventArtistMapping> artists =
                artistMappingRepository.findByEventId(eventId);

        return mapper.toResponse(e, mappings, benefits, artists);
    }

    @Transactional
    public void addBookmark(Long eventId, UUID userId) {
        // 이미 북마크했는지 체크
        if (eventBookmarkRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new IllegalStateException("이미 북마크된 이벤트입니다.");
        }

        // 이벤트, 유저 존재 확인
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("이벤트를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        EventBookmark bookmark = EventBookmark.builder()
                .event(event)
                .user(user)
                .build();

        eventBookmarkRepository.save(bookmark);
    }

    @Transactional
    public void removeBookmark(Long eventId, UUID userId) {
        EventBookmark bookmark = eventBookmarkRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("북마크가 존재하지 않습니다."));

        eventBookmarkRepository.delete(bookmark);
    }

    @Transactional(readOnly = true)
    public Page<EventInfoDto> search(UUID userId, String query, Pageable pageable) {
        return eventRepository.searchByTitle(query, pageable)
                .map(event -> EventInfoDto.builder()
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
                        .bookmarkCount(event.getBookmarkCount())
                        .bookmarked(
                                userId != null &&
                                        eventBookmarkRepository.existsByEventIdAndUserId(event.getId(), userId)
                        )
                        .bookmarked(false)
                        .build()
                );
    }
}
