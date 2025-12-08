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
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.AbstractImageSupport;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.image.service.ImageVariantService;
import com.dearwith.dearwith_backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class EventImageAppService extends AbstractImageSupport {

    private final AfterCommitExecutor localAfterCommitExecutor;
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
        this.localAfterCommitExecutor = afterCommitExecutor;
        this.eventImageMappingRepository = eventImageMappingRepository;
    }

    /**
     * 이벤트 생성 시 이미지 등록
     */
    @Transactional
    public void create(Event event, List<ImageAttachmentRequestDto> reqs, User user) {
        if (reqs == null || reqs.isEmpty()) {
            return;
        }

        validateTmpKeys(
                reqs.stream()
                        .map(ImageAttachmentRequestDto::tmpKey)
                        .toList()
        );

        Set<Integer> seen = new HashSet<>();
        for (ImageAttachmentRequestDto dto : reqs) {
            Integer ord = safeOrder(dto.displayOrder());
            if (!seen.add(ord)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        "이미지 등록 중 오류가 발생했습니다.",
                        "EVENT_IMAGE_DISPLAY_ORDER_DUPLICATED"
                );
            }
        }

        // --- 커버 이미지 후보 추적용 ---
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

            Image img = new Image();
            img.setUser(user);
            img.setS3Key(tmpKey);
            img.setStatus(ImageStatus.TMP);
            imageRepository.save(img);

            int ord = safeOrder(dto.displayOrder());

            EventImageMapping mapping = EventImageMapping.builder()
                    .image(img)
                    .event(event)
                    .displayOrder(ord)
                    .build();
            event.addImageMapping(mapping);

            created.add(new NewImage(img.getId(), tmpKey));

            // 🔹 displayOrder 가장 작은 이미지를 커버 후보로
            if (ord < minOrder) {
                minOrder = ord;
                coverCandidate = img;
            }
        }

        // 🔹 커버 이미지 세팅
        if (coverCandidate != null) {
            event.changeCoverImage(coverCandidate);
        }

        // 🔹 AfterCommit 에서 TMP → INLINE + variants 생성 (AssetOps 사용)
        for (NewImage ni : created) {
            localAfterCommitExecutor.run(() -> assetOps.commitExistingAndGenerateVariants(
                    AssetOps.CommitCommand.builder()
                            .imageId(ni.id())
                            .tmpKey(ni.tmpKey())
                            .userId(user.getId())
                            .preset(AssetVariantPreset.EVENT)
                            .build()
            ));
        }
    }

    /**
     * 이벤트 수정 시 이미지 일괄 갱신
     *  - reqs: 남길/추가할 이미지 전체 목록
     *  - 비어 있으면 모두 삭제
     */
    @Transactional
    public void update(Event event, List<ImageAttachmentUpdateRequestDto> reqs, UUID userId) {
        if (reqs == null) return;
        if (reqs.isEmpty()) {
            deleteAll(event.getId());
            // 🔹 이미지가 하나도 없으니 커버도 제거
            event.changeCoverImage(null);
            return;
        }

        validateTmpKeys(
                reqs.stream()
                        .map(ImageAttachmentUpdateRequestDto::tmpKey)
                        .toList()
        );

        Set<Integer> orders = new HashSet<>();
        for (var r : reqs) {
            Integer ord = safeOrder(r.displayOrder());
            if (!orders.add(ord)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        null,
                        "EVENT_IMAGE_DISPLAY_ORDER_DUPLICATED"
                );
            }

            boolean hasId  = r.id() != null;
            boolean hasTmp = hasTmp(r.tmpKey());
            if (hasId == hasTmp) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        null,
                        "EVENT_IMAGE_ID_OR_TMPKEY_XOR_REQUIRED"
                );
            }
        }

        List<EventImageMapping> beforeMappings = eventImageMappingRepository.findByEventId(event.getId());
        List<Long> beforeIds = beforeMappings.stream()
                .map(m -> m.getImage().getId())
                .toList();

        eventImageMappingRepository.deleteByEventId(event.getId());

        Map<Long, Integer> orderById = new HashMap<>();
        List<Long> finalIds = new ArrayList<>();

        // 기존 유지
        for (var r : reqs) {
            if (r.id() != null) {
                finalIds.add(r.id());
                orderById.put(r.id(), safeOrder(r.displayOrder()));
            }
        }

        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        // 신규 추가
        for (var r : reqs) {
            if (hasTmp(r.tmpKey())) {
                Image img = new Image();
                img.setUser(event.getUser());
                img.setS3Key(r.tmpKey());
                img.setStatus(ImageStatus.TMP);
                imageRepository.save(img);

                finalIds.add(img.getId());
                orderById.put(img.getId(), safeOrder(r.displayOrder()));

                created.add(new NewImage(img.getId(), r.tmpKey()));
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

        // 🔹 커버 이미지 다시 결정 (남아 있는 것 중 displayOrder 최소)
        Long coverImageId = finalIds.stream()
                .min(Comparator.comparing(orderById::get))
                .orElse(null);

        if (coverImageId != null) {
            event.changeCoverImage(imageRepository.getReferenceById(coverImageId));
        } else {
            event.changeCoverImage(null);
        }

        // after-commit: TMP → inline + variants
        for (NewImage ni : created) {
            localAfterCommitExecutor.run(() -> assetOps.commitExistingAndGenerateVariants(
                    AssetOps.CommitCommand.builder()
                            .imageId(ni.id())
                            .tmpKey(ni.tmpKey())
                            .userId(userId)
                            .preset(AssetVariantPreset.EVENT)
                            .build()
            ));
        }

        // 고아 처리
        Set<Long> finalSet = new HashSet<>(finalIds);
        List<Long> removed = beforeIds.stream()
                .filter(id -> !finalSet.contains(id))
                .toList();
        handleOrphans(removed);
    }

    /**
     * 이벤트에 연결된 이미지 전부 삭제
     */
    @Transactional
    public void deleteAll(Long eventId) {
        List<Long> before = eventImageMappingRepository.findByEventId(eventId)
                .stream()
                .map(m -> m.getImage().getId())
                .toList();

        eventImageMappingRepository.deleteByEventId(eventId);
        handleOrphans(before);
    }

    /**
     * 필요하다면 단일 이벤트 이미지 삭제용 메서드도 유지 (현재는 deleteAll과 동일)
     */
    @Transactional
    public void delete(Long eventId) {
        List<Long> before = eventImageMappingRepository.findByEventId(eventId)
                .stream()
                .map(m -> m.getImage().getId())
                .toList();

        eventImageMappingRepository.deleteByEventId(eventId);
        handleOrphans(before);
    }

    /**
     * 어떤 매핑에서도 쓰지 않는 이미지 → soft delete
     */
    private void handleOrphans(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return;
        for (Long id : imageIds) {
            if (eventImageMappingRepository.countUsages(id) == 0) {
                imageService.softDeleteIfNotYet(id);
            }
        }
    }
}