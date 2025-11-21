package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.entity.EventImageMapping;
import com.dearwith.dearwith_backend.event.repository.EventImageMappingRepository;
import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.external.aws.S3Waiter;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentUpdateRequestDto;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.image.service.ImageVariantService;
import com.dearwith.dearwith_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EventImageAppService {
    private final AssetOps assetOps;
    private final ImageRepository imageRepository;
    private final AfterCommitExecutor afterCommitExecutor;
    private final ImageService imageService;
    private final S3Waiter s3Waiter;
    private final EventImageMappingRepository eventImageMappingRepository;
    private final ImageVariantService imageVariantService;

    /**
     * 이벤트 생성 시 이미지 등록
     */
    @Transactional
    public void create(Event event, List<ImageAttachmentRequestDto> images, User user) {
        if (images == null || images.isEmpty()) {
            return;
        }

        // displayOrder 중복 체크
        Set<Integer> seen = new HashSet<>();
        for (ImageAttachmentRequestDto dto : images) {
            Integer ord = dto.displayOrder() == null ? 0 : dto.displayOrder();
            if (!seen.add(ord)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "displayOrder가 중복되었습니다.");
            }
        }

        // 1) 트랜잭션 안: Image(TMP) 저장 + 이벤트 매핑 생성
        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        for (ImageAttachmentRequestDto dto : images) {
            String tmpKey = dto.tmpKey();

            Image img = new Image();
            img.setUser(user);
            img.setS3Key(tmpKey);
            img.setStatus(ImageStatus.TMP);
            imageRepository.save(img);

            EventImageMapping mapping = EventImageMapping.builder()
                    .image(img)
                    .event(event)
                    .displayOrder(dto.displayOrder() == null ? 0 : dto.displayOrder())
                    .build();
            event.addImageMapping(mapping);

            created.add(new NewImage(img.getId(), tmpKey));
        }

        // 2) 트랜잭션 커밋 후: tmp→inline 승격 + URL 반영 + 파생본(이벤트 프리셋) 생성
        for (NewImage ni : created) {
            afterCommitExecutor.run(() -> {
                String inlineKey = imageService.promoteAndCommit(ni.id(), ni.tmpKey());

                s3Waiter.waitUntilExists(inlineKey);

                // 이벤트용 프리셋으로 변경 (프로젝트에서 사용하는 값으로 맞춰줘)
                imageVariantService.generateVariants(inlineKey, AssetVariantPreset.EVENT);
            });
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
            return;
        }

        // displayOrder & 요청 형식 검증
        Set<Integer> orders = new HashSet<>();
        for (var r : reqs) {
            if (!orders.add(r.displayOrder())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "displayOrder가 중복되었습니다.");
            }

            boolean hasId  = r.id() != null;
            boolean hasTmp = r.tmpKey() != null && !r.tmpKey().isBlank();
            if (hasId == hasTmp) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "각 항목은 id 또는 tmpKey 중 하나만 제공해야 합니다.");
            }
        }

        // 1) 이전 스냅샷
        List<EventImageMapping> beforeMappings = eventImageMappingRepository.findByEventId(event.getId());
        List<Long> beforeIds = beforeMappings.stream()
                .map(m -> m.getImage().getId())
                .toList();

        // 2) 기존 매핑 삭제
        eventImageMappingRepository.deleteByEventId(event.getId());

        // 3) 최종 이미지 id/순서 구성
        Map<Long, Integer> orderById = new HashMap<>();
        List<Long> finalIds = new ArrayList<>();

        // 3-1) 기존 유지(id)
        for (var r : reqs) {
            if (r.id() != null) {
                finalIds.add(r.id());
                orderById.put(r.id(), r.displayOrder());
            }
        }

        // 3-2) 신규 추가(tmpKey)
        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        for (var r : reqs) {
            if (r.tmpKey() != null && !r.tmpKey().isBlank()) {
                Image img = new Image();
                img.setUser(event.getUser()); // 이벤트 작성자/주최자 필드에 맞게 수정
                img.setS3Key(r.tmpKey());
                img.setStatus(ImageStatus.TMP);
                imageRepository.save(img);

                finalIds.add(img.getId());
                orderById.put(img.getId(), r.displayOrder());

                created.add(new NewImage(img.getId(), r.tmpKey()));
            }
        }

        // 4) 매핑 재생성
        for (Long imageId : finalIds) {
            EventImageMapping m = EventImageMapping.builder()
                    .event(event)
                    .image(imageRepository.getReferenceById(imageId))
                    .displayOrder(orderById.get(imageId))
                    .build();
            eventImageMappingRepository.save(m);
        }

        // 5) after-commit: TMP → inline 승격 + 파생본 생성(이벤트 프리셋)
        for (NewImage ni : created) {
            afterCommitExecutor.run(() -> assetOps.commitExistingAndGenerateVariants(
                    AssetOps.CommitCommand.builder()
                            .imageId(ni.id())
                            .tmpKey(ni.tmpKey())
                            .userId(userId)
                            .preset(AssetVariantPreset.EVENT)
                            .build()
            ));
        }

        // 6) 고아 처리 (before - final)
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