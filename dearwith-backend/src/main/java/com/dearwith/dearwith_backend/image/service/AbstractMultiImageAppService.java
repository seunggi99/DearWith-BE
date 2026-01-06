package com.dearwith.dearwith_backend.image.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractMultiImageAppService extends AbstractImageSupport {

    protected AbstractMultiImageAppService(
            TmpImageGuard tmpImageGuard,
            ImageRepository imageRepository,
            AfterCommitExecutor afterCommitExecutor,
            AssetOps assetOps,
            ImageService imageService
    ) {
        super(tmpImageGuard, imageRepository, afterCommitExecutor, assetOps, imageService);
    }

    /**
     * id / tmpKey / displayOrder 를 가진 DTO 공통 인터페이스
     * (이벤트/리뷰 각각의 Update DTO가 이 인터페이스를 구현하게 하면 재사용 가능)
     */
    public interface IdTmpOrder {
        Long id();
        String tmpKey();
        Integer displayOrder();
    }

    /**
     * id, tmpKey 가 XOR 조건을 만족하는지 검증
     *  - id만 있거나, tmpKey만 있어야 하고
     *  - 둘 다 있거나, 둘 다 없으면 안 됨
     */
    protected void validateXorRule(IdTmpOrder r, String errorCode) {
        boolean hasId  = r.id() != null;
        boolean hasTmp = hasTmp(r.tmpKey());

        if (hasId == hasTmp) {
            // 둘 다 true이거나, 둘 다 false인 경우
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    null,
                    errorCode
            );
        }
    }

    /**
     * 여러 이미지 갱신을 위한 준비 단계 헬퍼
     * - DTO 목록(reqs)과 기존 이미지(beforeImages)를 바탕으로
     *   최종 이미지 목록 / 새로 만든 이미지 / 제거 대상 imageId 를 계산
     *
     * 실제 도메인(이벤트/리뷰)은 이 결과를 가지고
     * 매핑/순서/엔티티 세팅을 직접 처리하면 됨.
     */
    protected MultiImageUpdateResult prepareMultiImageUpdate(
            List<? extends IdTmpOrder> reqs,
            List<Image> beforeImages,
            User owner,
            String xorErrorCode,
            String orderErrorCode
    ) {
        if (reqs == null) {
            return new MultiImageUpdateResult(List.of(), List.of(), List.of());
        }

        // 1) tmpKey 검증
        validateTmpKeys(
                reqs.stream()
                        .map(IdTmpOrder::tmpKey)
                        .collect(Collectors.toList())
        );

        // 2) displayOrder 중복 검증
        validateDisplayOrders(
                reqs.stream()
                        .map(IdTmpOrder::displayOrder)
                        .collect(Collectors.toList()),
                orderErrorCode
        );

        // 3) XOR(id/tmpKey) 검증
        reqs.forEach(r -> validateXorRule(r, xorErrorCode));

        // 4) 기존 이미지 맵
        Map<Long, Image> beforeMap = beforeImages == null
                ? Map.of()
                : beforeImages.stream().collect(Collectors.toMap(Image::getId, img -> img));

        List<Image> finalImages = new ArrayList<>();
        List<Image> createdImages = new ArrayList<>();
        Set<Long> keptIds = new HashSet<>();

        for (IdTmpOrder r : reqs) {
            Long id = r.id();
            String tmpKey = r.tmpKey();

            boolean hasId  = id != null;
            boolean hasTmp = hasTmp(tmpKey);

            if (hasId && !hasTmp) {
                // 기존 유지
                Image existing = beforeMap.get(id);
                if (existing == null) {
                    throw BusinessException.withMessageAndDetail(
                            ErrorCode.NOT_FOUND,
                            "이미지 등록 중 오류가 발생했습니다.",
                            "IMAGE_NOT_FOUND:" + id
                    );
                }
                finalImages.add(existing);
                keptIds.add(id);
            } else if (!hasId && hasTmp) {
                // 신규 추가
                Image img = createTmpImage(tmpKey, owner);
                finalImages.add(img);
                createdImages.add(img);
                keptIds.add(img.getId());
            }
            // hasId && hasTmp / !hasId && !hasTmp 는 위 XOR 검증에서 이미 걸러짐
        }

        // 5) 삭제 대상 계산
        List<Long> removedIds = beforeImages == null
                ? List.of()
                : beforeImages.stream()
                .map(Image::getId)
                .filter(id -> !keptIds.contains(id))
                .collect(Collectors.toList());

        return new MultiImageUpdateResult(finalImages, createdImages, removedIds);
    }

    /**
     * after-commit 에서 새로 생성된 이미지들만 커밋
     */
    protected void commitNewImages(
            String logTag,
            List<Image> createdImages,
            UUID userId,
            AssetVariantPreset preset
    ) {
        if (createdImages == null || createdImages.isEmpty()) return;

        for (Image img : createdImages) {
            commitAfterTransaction(logTag, img.getId(), img.getS3Key(), userId, preset);
        }
    }

    /**
     * 멀티 이미지 갱신 결과 DTO
     */
    protected record MultiImageUpdateResult(
            List<Image> finalImages,
            List<Image> createdImages,
            List<Long> removedImageIds
    ) {}
}