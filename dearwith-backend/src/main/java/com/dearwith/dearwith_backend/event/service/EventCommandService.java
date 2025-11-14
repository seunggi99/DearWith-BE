package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.service.ArtistService;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.dto.EventCreateRequestDto;
import com.dearwith.dearwith_backend.event.dto.EventResponseDto;
import com.dearwith.dearwith_backend.event.entity.*;
import com.dearwith.dearwith_backend.event.enums.BenefitType;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import com.dearwith.dearwith_backend.event.mapper.EventMapper;
import com.dearwith.dearwith_backend.event.repository.EventArtistGroupMappingRepository;
import com.dearwith.dearwith_backend.event.repository.EventArtistMappingRepository;
import com.dearwith.dearwith_backend.event.repository.EventBenefitRepository;
import com.dearwith.dearwith_backend.event.repository.EventImageMappingRepository;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.external.x.XVerifyPayload;
import com.dearwith.dearwith_backend.external.x.XVerifyTicketService;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import com.dearwith.dearwith_backend.image.service.ImageAttachmentService;
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

        // 6. 응답 DTO 구성 (기존 로직 그대로 유지)
        List<EventImageMapping> mappings =
                mappingRepository.findByEvent_IdOrderByDisplayOrderAsc(saved.getId());
        List<EventBenefit> benefits =
                benefitRepository.findByEvent_IdOrderByDayIndexAscDisplayOrderAsc(saved.getId());
        List<EventArtistMapping> artists =
                eventArtistMappingRepository.findByEventId(saved.getId());
        List<EventArtistGroupMapping> artistGroups =
                eventArtistGroupMappingRepository.findByEventId(saved.getId());

        return mapper.toResponse(saved, mappings, benefits, artists, artistGroups);
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