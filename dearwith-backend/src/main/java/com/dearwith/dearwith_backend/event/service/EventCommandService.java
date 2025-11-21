package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.service.ArtistService;
import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.dto.EventCreateRequestDto;
import com.dearwith.dearwith_backend.event.dto.EventResponseDto;
import com.dearwith.dearwith_backend.event.dto.EventUpdateRequestDto;
import com.dearwith.dearwith_backend.event.entity.*;
import com.dearwith.dearwith_backend.event.enums.BenefitType;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import com.dearwith.dearwith_backend.event.mapper.EventMapper;
import com.dearwith.dearwith_backend.event.repository.*;
import com.dearwith.dearwith_backend.external.x.XVerifyPayload;
import com.dearwith.dearwith_backend.external.x.XVerifyTicketService;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentUpdateRequestDto;
import com.dearwith.dearwith_backend.image.service.ImageAttachmentService;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final EventImageMappingRepository mappingRepository;
    private final EventBenefitRepository benefitRepository;
    private final EventMapper mapper;
    private final ArtistService artistService;
    private final XVerifyTicketService xVerifyTicketService;
    private final ImageAttachmentService imageAttachmentService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final EventImageAppService eventImageAppService;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final EventNoticeRepository eventNoticeRepository;
    private final EventQueryService eventQueryService;

    @PersistenceContext
    private final EntityManager em;

    @Transactional
    public EventResponseDto create(UUID userId, EventCreateRequestDto req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "유저를 찾을 수 없습니다."));

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
            //imageAttachmentService.setEventImages(event, imageDtos, userId);
            eventImageAppService.create(event, imageDtos, user);
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

        return eventQueryService.getEvent(saved.getId(), userId);
    }

    @Transactional
    public EventResponseDto update(Long eventId, UUID userId, EventUpdateRequestDto req){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이벤트를 찾을 수 없습니다."));

        authService.validateOwner(event.getUser(), userId, "이벤트 수정 권한이 없습니다.");

        // 0) 이미지 개수 제한 (10개)
        if (req.images() != null && req.images().size() > 10) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이벤트 이미지는 최대 10개까지 등록할 수 있습니다.");
        }

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
            EventUpdateRequestDto.PlaceDto p = req.place();

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

            // 중복 방어
            Set<Long> uniqueArtistIds = new LinkedHashSet<>(artistIds);
            if (artistIds.size() != uniqueArtistIds.size()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "artistIds에 중복된 값이 포함되어 있습니다.");
            }

            if (uniqueArtistIds.isEmpty()) {
                event.getArtists().clear();
                em.flush();
            } else {
                // 1) 존재 여부 검증
                var artists = artistService.findAllByIds(new ArrayList<>(uniqueArtistIds));
                var foundIds = artists.stream().map(Artist::getId).collect(Collectors.toSet());
                var missing = uniqueArtistIds.stream().filter(id -> !foundIds.contains(id)).toList();
                if (!missing.isEmpty()) {
                    throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND, "존재하지 않는 아티스트 ID: " + missing);
                }

                // 2) 기존 매핑 제거 + flush (UK 충돌 방지)
                event.getArtists().clear();
                em.flush();

                // 3) 새 매핑 추가
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

            if (groupIds.size() != uniqueGroupIds.size()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "artistGroupIds에 중복된 값이 포함되어 있습니다.");
            }

            if (uniqueGroupIds.isEmpty()) {
                event.getArtistGroups().clear();
                em.flush();
            } else {
                event.getArtistGroups().clear();
                em.flush();

                for (Long groupId : uniqueGroupIds) {
                    ArtistGroup group = artistGroupRepository.findById(groupId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "아티스트 그룹을 찾을 수 없습니다. id=" + groupId));

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
                    throw new BusinessException(ErrorCode.EVENT_START_REQUIRED);
                }

                for (EventUpdateRequestDto.BenefitDto b : req.benefits()) {
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
        }

        // 4) 이미지 업데이트 (리뷰와 동일 패턴: null=변경없음, []=전부삭제, 값=재구성)
        if (req.images() != null) {
            if (req.images().isEmpty()) {
                eventImageAppService.deleteAll(eventId);
            } else {
                // id / tmpKey XOR 체크는 EventImageAppService.update 안에서 이미 하고 있음
                List<ImageAttachmentUpdateRequestDto> imageReqs = req.images().stream()
                        .map(d -> new ImageAttachmentUpdateRequestDto(d.id(), d.tmpKey(), d.displayOrder()))
                        .toList();

                eventImageAppService.update(event, imageReqs, userId);
            }
        }

        // 5) 상태(status) 재계산
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate start = event.getStartDate();
        LocalDate end   = event.getEndDate();

        if (start == null) {
            throw new BusinessException(ErrorCode.EVENT_START_REQUIRED);
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
        Event saved = eventRepository.save(event);

        return eventQueryService.getEvent(saved.getId(), userId);
    }
    @Transactional
    public void delete(Long eventId, UUID userId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이벤트를 찾을 수 없습니다."));

        authService.validateOwner(event.getUser(), userId, "이벤트 삭제 권한이 없습니다.");

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
}