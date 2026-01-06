package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.entity.EventImageMapping;
import com.dearwith.dearwith_backend.event.repository.EventImageMappingRepository;
import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentUpdateRequestDto;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.AbstractImageSupport;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class EventImageAppService extends AbstractImageSupport {

    private static final String LOG_TAG = "event-image";
    private final EventImageMappingRepository eventImageMappingRepository;

    public EventImageAppService(
            TmpImageGuard tmpImageGuard,
            ImageRepository imageRepository,
            AfterCommitExecutor afterCommitExecutor,
            AssetOps assetOps,
            ImageService imageService,
            EventImageMappingRepository eventImageMappingRepository
    ) {
        super(tmpImageGuard, imageRepository, afterCommitExecutor, assetOps, imageService);
        this.eventImageMappingRepository = eventImageMappingRepository;
    }

    @Transactional
    public void create(Event event, List<ImageAttachmentRequestDto> reqs, User user) {
        if (reqs == null || reqs.isEmpty()) return;

        validateTmpKeys(reqs.stream().map(ImageAttachmentRequestDto::tmpKey).toList());
        requireTmpPrefixAll(reqs.stream().map(ImageAttachmentRequestDto::tmpKey).toList(),
                "EVENT_IMAGE_TMPKEY_INVALID");

        validateDisplayOrders(
                reqs.stream().map(ImageAttachmentRequestDto::displayOrder).toList(),
                "EVENT_IMAGE_DISPLAY_ORDER_DUPLICATED"
        );

        Image coverCandidate = null;
        int minOrder = Integer.MAX_VALUE;

        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        for (ImageAttachmentRequestDto dto : reqs) {
            String tmpKey = dto.tmpKey();
            if (!hasTmp(tmpKey)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        "이미지 등록 중 오류가 발생했습니다.",
                        "EVENT_IMAGE_TMPKEY_EMPTY"
                );
            }
            requireTmpPrefix(tmpKey, "EVENT_IMAGE_TMPKEY_INVALID");

            Image img = createTmpImage(tmpKey, user);

            int ord = safeOrder(dto.displayOrder());
            EventImageMapping mapping = EventImageMapping.builder()
                    .image(img)
                    .event(event)
                    .displayOrder(ord)
                    .build();
            event.addImageMapping(mapping);

            created.add(new NewImage(img.getId(), tmpKey));

            if (ord < minOrder) {
                minOrder = ord;
                coverCandidate = img;
            }
        }

        event.changeCoverImage(coverCandidate);

        for (NewImage ni : created) {
            commitAfterTransaction(LOG_TAG, ni.id(), ni.tmpKey(), user.getId(), AssetVariantPreset.EVENT);
        }
    }

    @Transactional
    public void update(Event event, List<ImageAttachmentUpdateRequestDto> reqs, UUID userId) {
        if (reqs == null) return;

        if (reqs.isEmpty()) {
            deleteAll(event.getId());
            event.changeCoverImage(null);
            return;
        }

        validateTmpKeys(reqs.stream().map(ImageAttachmentUpdateRequestDto::tmpKey).toList());

        validateDisplayOrders(
                reqs.stream().map(ImageAttachmentUpdateRequestDto::displayOrder).toList(),
                "EVENT_IMAGE_DISPLAY_ORDER_DUPLICATED"
        );

        // 요청 형식 검증 + 존재 검증 + tmp/ prefix 검증
        for (var r : reqs) {
            boolean hasId  = r.id() != null;
            boolean hasTmp = hasTmp(r.tmpKey());

            if (hasId == hasTmp) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        "이미지 등록 중 오류가 발생했습니다.",
                        "EVENT_IMAGE_ID_OR_TMPKEY_XOR_REQUIRED"
                );
            }

            if (hasId) {
                ensureImageOwnedBy(r.id(), event.getUser().getId(), "EVENT_IMAGE_ID_NOT_OWNED");
            } else {
                requireTmpPrefix(r.tmpKey(), "EVENT_IMAGE_TMPKEY_INVALID");
            }
        }

        // before snapshot
        List<EventImageMapping> beforeMappings =
                eventImageMappingRepository.findByEventId(event.getId());
        List<Long> beforeIds = beforeMappings.stream()
                .map(m -> m.getImage().getId())
                .toList();

        eventImageMappingRepository.deleteByEventId(event.getId());

        Map<Long, Integer> orderById = new HashMap<>();
        List<Long> finalIds = new ArrayList<>();

        // 기존 유지
        for (var r : reqs) {
            if (r.id() != null) {
                Long id = r.id();
                finalIds.add(id);
                orderById.put(id, safeOrder(r.displayOrder()));
            }
        }

        // 신규 추가
        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        for (var r : reqs) {
            if (hasTmp(r.tmpKey())) {
                String tmpKey = r.tmpKey();
                requireTmpPrefix(tmpKey, "EVENT_IMAGE_TMPKEY_INVALID");

                Image img = createTmpImage(tmpKey, event.getUser()); // 주최자 기준
                Long id = img.getId();

                finalIds.add(id);
                orderById.put(id, safeOrder(r.displayOrder()));

                created.add(new NewImage(id, tmpKey));
            }
        }

        // 매핑 재생성
        for (Long imageId : finalIds) {
            EventImageMapping m = EventImageMapping.builder()
                    .event(event)
                    .image(imageRepository.getReferenceById(imageId))
                    .displayOrder(orderById.get(imageId))
                    .build();
            eventImageMappingRepository.save(m);
        }

        // 커버 이미지 재결정(최소 displayOrder)
        Long coverImageId = finalIds.stream()
                .min(Comparator.comparing(orderById::get))
                .orElse(null);

        event.changeCoverImage(coverImageId == null ? null : imageRepository.getReferenceById(coverImageId));

        // after-commit 커밋
        for (NewImage ni : created) {
            commitAfterTransaction(LOG_TAG, ni.id(), ni.tmpKey(), userId, AssetVariantPreset.EVENT);
        }

        // orphan 처리
        Set<Long> finalSet = new HashSet<>(finalIds);
        List<Long> removed = beforeIds.stream()
                .filter(id -> !finalSet.contains(id))
                .toList();

        handleOrphans(removed, eventImageMappingRepository::countUsages);
    }

    @Transactional
    public void deleteAll(Long eventId) {
        List<Long> before = eventImageMappingRepository.findByEventId(eventId)
                .stream()
                .map(m -> m.getImage().getId())
                .toList();

        eventImageMappingRepository.deleteByEventId(eventId);
        handleOrphans(before, eventImageMappingRepository::countUsages);
    }

    @Transactional
    public void delete(Long eventId) {
        List<Long> before = eventImageMappingRepository.findByEventId(eventId)
                .stream()
                .map(m -> m.getImage().getId())
                .toList();

        eventImageMappingRepository.deleteByEventId(eventId);
        handleOrphans(before, eventImageMappingRepository::countUsages);
    }
}