package com.dearwith.dearwith_backend.image.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractImageSupport {

    protected final TmpImageGuard tmpImageGuard;
    protected final ImageRepository imageRepository;
    protected final AfterCommitExecutor afterCommitExecutor;
    protected final AssetOps assetOps;
    protected final ImageService imageService;

    /* ========== tmpKey 검증 ========== */

    /** 단일 tmpKey 검증 */
    protected void validateTmpKey(String tmpKey) {
        tmpImageGuard.ensureExists(tmpKey);
    }

    /** 여러 tmpKey 검증 (null/blank 제외 + 중복 제거) */
    protected void validateTmpKeys(Collection<String> tmpKeys) {
        if (tmpKeys == null || tmpKeys.isEmpty()) return;

        List<String> distinct =
                tmpKeys.stream()
                        .filter(this::hasTmp)
                        .distinct()
                        .collect(Collectors.toList());

        if (!distinct.isEmpty()) {
            tmpImageGuard.ensureAllExists(distinct);
        }
    }

    /* ========== TMP 이미지 생성 ========== */

    /** owner 없는 이미지 생성은 위험하므로 반드시 방어 */
    protected Image createTmpImage(String tmpKey, User owner) {
        if (!hasTmp(tmpKey)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "이미지 등록 중 오류가 발생했습니다.",
                    "IMAGE_TMP_KEY_EMPTY"
            );
        }

        if (owner == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "이미지 등록 중 오류가 발생했습니다.",
                    "IMAGE_OWNER_REQUIRED"
            );
        }

        Image img = new Image();
        img.setUser(owner);
        img.setS3Key(tmpKey);
        img.setStatus(ImageStatus.TMP);

        imageRepository.save(img);
        return img;
    }

    /* ========== after-commit 커밋 ========== */

    protected void commitAfterTransaction(
            Long imageId,
            String tmpKey,
            UUID userId,
            AssetVariantPreset preset
    ) {
        if (!hasTmp(tmpKey)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "이미지 등록 중 오류가 발생했습니다.",
                    "IMAGE_COMMIT_TMPKEY_EMPTY"
            );
        }

        if (preset == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.IMAGE_PROCESSING_FAILED,
                    "이미지 등록 중 오류가 발생했습니다.",
                    "IMAGE_PRESET_NULL"
            );
        }

        afterCommitExecutor.run(() -> assetOps.commitExistingAndGenerateVariants(
                AssetOps.CommitCommand.builder()
                        .imageId(imageId)
                        .tmpKey(tmpKey)
                        .userId(userId)
                        .preset(preset)
                        .build()
        ));
    }

    /* ========== orphan 처리 ========== */

    @FunctionalInterface
    protected interface LongCountFunction {
        long count(Long id);
    }

    /**
     * imageIds 중에서 더 이상 어떤 매핑에서도 쓰이지 않는 이미지에 대해 soft delete 수행
     */
    protected void handleOrphans(Collection<Long> imageIds, LongCountFunction countUsageFn) {
        if (imageIds == null || imageIds.isEmpty()) return;

        for (Long id : imageIds) {
            if (countUsageFn.count(id) == 0) {
                imageService.softDeleteIfNotYet(id);
            }
        }
    }

    /* ========== 공통 유틸 ========== */

    protected boolean hasTmp(String tmpKey) {
        return tmpKey != null && !tmpKey.isBlank();
    }

    protected int safeOrder(Integer ord) {
        return ord == null ? 0 : ord;
    }

    /**
     * displayOrder 중복 검증 (null은 0 취급)
     */
    protected void validateDisplayOrders(Collection<Integer> orders, String errorCode) {
        if (orders == null) return;

        Set<Integer> seen = new HashSet<>();
        for (Integer ord : orders) {
            int v = safeOrder(ord);
            if (!seen.add(v)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        "이미지 등록 중 오류가 발생했습니다.",
                        errorCode
                );
            }
        }
    }
}