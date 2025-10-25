package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.artist.entity.Artist;
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
import com.dearwith.dearwith_backend.image.Image;
import com.dearwith.dearwith_backend.image.ImageAttachmentRequest;
import com.dearwith.dearwith_backend.image.ImageAttachmentService;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final EventArtistMappingRepository artistMappingRepository;
    private final ArtistService artistService;
    private final XVerifyTicketService xVerifyTicketService;
    private final ImageAttachmentService imageAttachmentService;

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
        if (event.getStatus() == null) event.setStatus(EventStatus.SCHEDULED);

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
                    .map(d -> new ImageAttachmentRequest(d.tmpKey(), d.displayOrder()))
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

        validateAndNormalize(event);

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
            throw new BusinessException(ErrorCode.ALREADY_BOOKMARKED_EVENT);
        }

        // 이벤트, 유저 존재 확인
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKMARK_NOT_FOUND));

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
                        ? eventBookmarkRepository.findByUserIdAndEvent_Status(userId, statusFilter, pageable)
                        : eventBookmarkRepository.findByUserId(userId, pageable);

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
                    .bookmarkCount(event.getBookmarkCount())
                    .bookmarked(true)
                    .build();
        });
    }

}
