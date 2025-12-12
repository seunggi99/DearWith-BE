package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.service.ArtistService;
import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.common.dto.CreatedResponseDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.dto.EventCreateRequestDto;
import com.dearwith.dearwith_backend.event.dto.EventUpdateRequestDto;
import com.dearwith.dearwith_backend.event.entity.*;
import com.dearwith.dearwith_backend.event.enums.BenefitType;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import com.dearwith.dearwith_backend.event.enums.EventType;
import com.dearwith.dearwith_backend.event.mapper.EventMapper;
import com.dearwith.dearwith_backend.event.repository.*;
import com.dearwith.dearwith_backend.external.x.XVerifyPayload;
import com.dearwith.dearwith_backend.external.x.XVerifyTicketService;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentUpdateRequestDto;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.service.UserReader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventCommandService {

    private final EventRepository eventRepository;
    private final ArtistGroupRepository artistGroupRepository;
    private final EventArtistMappingRepository eventArtistMappingRepository;
    private final EventArtistGroupMappingRepository eventArtistGroupMappingRepository;
    private final EventBenefitRepository benefitRepository;
    private final EventMapper mapper;
    private final ArtistService artistService;
    private final XVerifyTicketService xVerifyTicketService;
    private final AuthService authService;
    private final EventImageAppService eventImageAppService;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final EventNoticeRepository eventNoticeRepository;
    private final UserReader userReader;

    @PersistenceContext
    private final EntityManager em;

    /*──────────────────────────────────────────────
     | 1. 이벤트 생성
     *──────────────────────────────────────────────*/
    @CacheEvict(cacheNames = "thisMonthAnniversaries", allEntries = true)
    @Transactional
    public CreatedResponseDto create(UUID userId, EventCreateRequestDto req) {

        User user = userReader.getActiveUser(userId);

        // 0) X 인증 티켓 확인/소모
        XVerifyPayload xPayload = null;
        if (req.organizer().xTicket() != null && !req.organizer().xTicket().isBlank()) {
            try {
                xPayload = xVerifyTicketService.confirmAndConsume(req.organizer().xTicket(), user.getId());
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw BusinessException.withAll(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        null,
                        "X_TICKET_VERIFY_FAILED",
                        "X ticket verify failed: " + e.getMessage(),
                        e
                );
            }
        }

        // 1) Event 매핑 + 날짜 검증 (USER 오류)
        Event event = mapper.toEvent(user.getId(), req);
        if (event.getStartDate() == null) {
            throw BusinessException.of(ErrorCode.EVENT_START_REQUIRED);
        }
        if (event.getEndDate() != null && event.getEndDate().isBefore(event.getStartDate())) {
            throw BusinessException.of(ErrorCode.EVENT_DATE_RANGE_INVALID);
        }

        LocalTime open = event.getOpenTime();
        LocalTime close = event.getCloseTime();

        if (open != null && close != null) {
            if (open.isAfter(close)) {
                throw BusinessException.withMessage(
                        ErrorCode.INVALID_TIME_RANGE,
                        "시작 시간을 다시 입력해주세요."
                );
            }

            if (close.isBefore(open)) {
                throw BusinessException.withMessage(
                        ErrorCode.INVALID_TIME_RANGE,
                        "종료 시간을 다시 입력해주세요."
                );
            }
        }

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
        event.setEventType(EventType.BIRTHDAY_CAFE);

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
            eventImageAppService.create(event, imageDtos, user);
        }

        // 3. 특전 매핑
        if (req.benefits() != null) {
            LocalDate eventStart = event.getStartDate();
            if (eventStart == null) {
                throw BusinessException.of(ErrorCode.EVENT_START_REQUIRED);
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
                        throw BusinessException.of(ErrorCode.BENEFIT_DAYINDEX_INVALID);
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
            List<Long> artistIds = req.artistIds();
            Set<Long> uniqueArtistIds = new LinkedHashSet<>(artistIds);

            if (artistIds.size() != uniqueArtistIds.size()) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        null,
                        "ARTIST_ID_DUPLICATED"
                );
            }

            List<Artist> artists = artistService.findAllByIds(new ArrayList<>(uniqueArtistIds));

            Set<Long> foundIds = artists.stream().map(Artist::getId).collect(Collectors.toSet());
            List<Long> missing = uniqueArtistIds.stream().filter(id -> !foundIds.contains(id)).toList();
            if (!missing.isEmpty()) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.ARTIST_NOT_FOUND,
                        ErrorCode.ARTIST_NOT_FOUND.getMessage(),
                        "MISSING_ARTIST_IDS"
                );
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
            List<Long> groupIds = req.artistGroupIds();
            Set<Long> uniqueGroupIds = new LinkedHashSet<>(groupIds);

            // FRONT 오류: 중복된 그룹 ID
            if (groupIds.size() != uniqueGroupIds.size()) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        null,
                        "ARTIST_GROUP_ID_DUPLICATED"
                );
            }

            for (Long groupId : uniqueGroupIds) {
                ArtistGroup group = artistGroupRepository.findById(groupId)
                        .orElseThrow(() -> BusinessException.withMessageAndDetail(
                                ErrorCode.GROUP_NOT_FOUND,
                                "존재하지 않는 아티스트 그룹입니다.",
                                "GROUP_NOT_FOUND"
                        ));

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

        return CreatedResponseDto.builder()
                .id(saved.getId())
                .build();
    }



    /*──────────────────────────────────────────────
     | 2. 이벤트 수정
     *──────────────────────────────────────────────*/
    @Transactional
    public void update(Long eventId, UUID userId, EventUpdateRequestDto req) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "이벤트를 찾을 수 없습니다."
                ));

        User user = userReader.getActiveUser(userId);
        authService.validateOwner(event.getUser(), user, "이벤트 수정 권한이 없습니다.");

        // 1) 기본 필드 업데이트 (null 이면 변경 안 함)
        if (req.title() != null) {
            event.setTitle(req.title().trim());
        }
        if (req.openTime() != null) {
            event.setOpenTime(req.openTime());
        }
        if (req.closeTime() != null) {
            event.setCloseTime(req.closeTime());
        }
        if (req.startDate() != null) {
            event.setStartDate(req.startDate());
        }
        if (req.endDate() != null) {
            event.setEndDate(req.endDate());
        }

        // 1-1) 장소 정보 업데이트
        if (req.place() != null) {
            EventCreateRequestDto.PlaceDto p = req.place();

            var place = event.getPlaceInfo();
            if (place == null) {
                place = new PlaceInfo();
            }
            place.setKakaoPlaceId(p.kakaoPlaceId());
            place.setName(p.name());
            place.setRoadAddress(p.roadAddress());
            place.setJibunAddress(p.jibunAddress());
            place.setLon(p.lon());
            place.setLat(p.lat());
            place.setPhone(p.phone());
            place.setPlaceUrl(p.placeUrl());

            event.setPlaceInfo(place);
        }

        // 2) 아티스트 매핑 (null: 변경 없음, []: 모두 제거, 값: 전부 교체)
        if (req.artistIds() != null) {
            List<Long> artistIds = req.artistIds();
            Set<Long> uniqueArtistIds = new LinkedHashSet<>(artistIds);

            if (artistIds.size() != uniqueArtistIds.size()) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        null,
                        "ARTIST_ID_DUPLICATED"
                );
            }

            if (uniqueArtistIds.isEmpty()) {
                event.getArtists().clear();
                em.flush();
            } else {
                var artists = artistService.findAllByIds(new ArrayList<>(uniqueArtistIds));
                var foundIds = artists.stream().map(Artist::getId).collect(Collectors.toSet());
                var missing = uniqueArtistIds.stream().filter(id -> !foundIds.contains(id)).toList();
                if (!missing.isEmpty()) {
                    // USER 오류
                    throw BusinessException.withMessageAndDetail(
                            ErrorCode.ARTIST_NOT_FOUND,
                            ErrorCode.ARTIST_NOT_FOUND.getMessage(),
                            "MISSING_ARTIST_IDS"
                    );
                }

                event.getArtists().clear();
                em.flush();

                for (Artist artist : artists) {
                    EventArtistMapping mapping = EventArtistMapping.builder()
                            .event(event)
                            .artist(artist)
                            .build();
                    event.addArtistMapping(mapping);
                }
            }
        }

        // 2-1) 아티스트 그룹 매핑 (null: 변경 없음, []: 모두 제거, 값: 전부 교체)
        if (req.artistGroupIds() != null) {
            List<Long> groupIds = req.artistGroupIds();
            Set<Long> uniqueGroupIds = new LinkedHashSet<>(groupIds);

            // FRONT 오류: 중복
            if (groupIds.size() != uniqueGroupIds.size()) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        null,
                        "ARTIST_GROUP_ID_DUPLICATED"
                );
            }

            if (uniqueGroupIds.isEmpty()) {
                event.getArtistGroups().clear();
                em.flush();
            } else {
                event.getArtistGroups().clear();
                em.flush();

                for (Long groupId : uniqueGroupIds) {
                    ArtistGroup group = artistGroupRepository.findById(groupId)
                            .orElseThrow(() -> BusinessException.withMessageAndDetail(
                                    ErrorCode.GROUP_NOT_FOUND,
                                    "존재하지 않는 아티스트 그룹입니다.",
                                    "GROUP_NOT_FOUND"
                            ));

                    EventArtistGroupMapping groupMapping = EventArtistGroupMapping.builder()
                            .event(event)
                            .artistGroup(group)
                            .build();
                    event.addArtistGroupMapping(groupMapping);
                }
            }
        }

        // 3) 특전(benefits) 갱신 (null: 변경 없음, []: 모두 제거, 값: 전부 교체)
        if (req.benefits() != null) {
            event.getBenefits().clear();

            if (!req.benefits().isEmpty()) {
                LocalDate eventStart = event.getStartDate();
                if (eventStart == null) {
                    throw BusinessException.of(ErrorCode.EVENT_START_REQUIRED);
                }

                for (EventCreateRequestDto.BenefitDto b : req.benefits()) {
                    int displayOrder = (b.displayOrder() != null) ? b.displayOrder() : 0;

                    Integer dayIndex = b.dayIndex();
                    LocalDate visibleFrom = null;

                    if (b.benefitType() == BenefitType.LIMITED) {
                        if (dayIndex == null) {
                            dayIndex = 1;
                        }
                        if (dayIndex < 1) {
                            throw BusinessException.of(ErrorCode.BENEFIT_DAYINDEX_INVALID);
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
        }

        // 4) 이미지 업데이트 (null=변경없음, []=전부삭제, 값=재구성)
        if (req.images() != null) {
            if (req.images().isEmpty()) {
                eventImageAppService.deleteAll(eventId);
            } else {
                List<ImageAttachmentUpdateRequestDto> imageReqs = req.images().stream()
                        .map(d -> new ImageAttachmentUpdateRequestDto(d.id(), d.tmpKey(), d.displayOrder()))
                        .toList();

                eventImageAppService.update(event, imageReqs, user.getId());
            }
        }

        // 5) 상태(status) 재계산 (USER 입력에 의해 달라지는 부분)
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate start = event.getStartDate();
        LocalDate end   = event.getEndDate();

        if (start == null) {
            throw BusinessException.of(ErrorCode.EVENT_START_REQUIRED);
        }

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

        // 6) 기타 도메인 정합성 체크
        validateAndNormalize(event);

        // 7) 저장
        eventRepository.save(event);
    }



    /*──────────────────────────────────────────────
     | 3. 이벤트 삭제 (Soft delete)
     *──────────────────────────────────────────────*/
    @Transactional
    public void delete(Long eventId, UUID userId) {
        User user = userReader.getLoginAllowedUser(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "이벤트를 찾을 수 없습니다."
                ));

        authService.validateOwner(event.getUser(), user, "이벤트 삭제 권한이 없습니다.");

        // 1) 이벤트 이미지 삭제(+ 고아 이미지 soft delete)
        eventImageAppService.deleteAll(eventId);

        // 2) 북마크 삭제
        eventBookmarkRepository.deleteByEventId(eventId);

        // 3) 특전 삭제
        benefitRepository.deleteByEventId(eventId);

        // 4) 아티스트 매핑 삭제
        eventArtistMappingRepository.deleteByEventId(eventId);

        // 5) 아티스트 그룹 매핑 삭제
        eventArtistGroupMappingRepository.deleteByEventId(eventId);

        // 6) 공지 삭제
        eventNoticeRepository.deleteByEventId(eventId);

        // 7) 소프트 삭제
        event.softDelete();
        eventRepository.save(event);
    }


    /*──────────────────────────────────────────────
     | 4. 도메인 정합성 검증 / 정규화
     *──────────────────────────────────────────────*/
    private void validateAndNormalize(Event event) {
        if (event.getStartDate() != null && event.getEndDate() != null
                && event.getEndDate().isBefore(event.getStartDate())) {
            throw BusinessException.of(ErrorCode.EVENT_DATE_RANGE_INVALID);
        }

        if (event.getOrganizer() == null) {
            event.setOrganizer(new OrganizerInfo());
        }
        event.getOrganizer().normalize();

        if (event.getTitle() != null) {
            event.setTitle(event.getTitle().trim());
        }
    }
}