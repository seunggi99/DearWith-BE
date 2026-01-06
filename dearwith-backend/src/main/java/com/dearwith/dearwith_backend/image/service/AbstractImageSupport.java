package com.dearwith.dearwith_backend.image.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.enums.ImageProcessStatus;
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractImageSupport {

    protected final TmpImageGuard tmpImageGuard;
    protected final ImageRepository imageRepository;
    protected final AfterCommitExecutor afterCommitExecutor;
    protected final AssetOps assetOps;
    protected final ImageService imageService;

    /* ========== tmpKey 검증 ========== */

    protected void validateTmpKey(String tmpKey) {
        tmpImageGuard.ensureExists(tmpKey);
    }

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

    /** tmp/ prefix 강제 */
    protected void requireTmpPrefix(String tmpKey, String errorDetail) {
        if (!hasTmp(tmpKey) || !tmpKey.startsWith("tmp/")) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_TMP_KEY,
                    "이미지 등록 중 오류가 발생했습니다.",
                    errorDetail
            );
        }
    }

    /** 여러 tmpKey에 대해 tmp/ prefix 강제 (null/blank 제외 + 중복 제거) */
    protected void requireTmpPrefixAll(Collection<String> tmpKeys, String errorDetail) {
        if (tmpKeys == null || tmpKeys.isEmpty()) return;

        for (String k : tmpKeys.stream().filter(this::hasTmp).distinct().toList()) {
            requireTmpPrefix(k, errorDetail);
        }
    }

    /* ========== id 존재 검증 ========== */

    protected void ensureImageExists(Long imageId, String errorDetail) {
        if (imageId == null || !imageRepository.existsById(imageId)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.NOT_FOUND,
                    "이미지 등록 중 오류가 발생했습니다.",
                    errorDetail
            );
        }
    }

    /* ========== TMP 이미지 생성 ========== */

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
        img.setProcessStatus(ImageProcessStatus.READY);
        imageRepository.save(img);
        return img;
    }

    /* ========== after-commit 커밋 ========== */
    protected void commitAfterTransaction(
            String logTag,
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

        // 1) 동기: tmp → inline 승격 + DB PROCESSING
        imageService.promoteAndCommit(imageId, tmpKey);

        // 2) 비동기: variants 생성 + READY/FAILED 마킹 (트랜잭션 커밋 이후)
        afterCommitExecutor.run(() -> {
            try {
                assetOps.generateVariantsAndMarkStatus(imageId, preset);
            } catch (Throwable t) {
                log.error("[{}] generateVariantsAndMarkStatus failed. imageId={}, tmpKey={}",
                        logTag, imageId, tmpKey, t);
            }
        });
    }

    protected void commitSingleAfterTransaction(
            String logTag,
            Long imageId,
            String tmpKey,
            UUID userId,
            AssetVariantPreset preset
    ) {
        commitAfterTransaction(logTag, imageId, tmpKey, userId, preset);
    }

    protected void generateVariantsAfterTransaction(
            String logTag,
            Long imageId,
            AssetVariantPreset preset
    ) {
        if (preset == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.IMAGE_PROCESSING_FAILED,
                    "이미지 등록 중 오류가 발생했습니다.",
                    "IMAGE_PRESET_NULL"
            );
        }

        afterCommitExecutor.run(() -> {
            try {
                assetOps.generateVariantsAndMarkStatus(imageId, preset);
            } catch (Throwable t) {
                log.error("[{}] generateVariantsAndMarkStatus failed. imageId={}", logTag, imageId, t);
            }
        });
    }

    /* ========== orphan 처리 ========== */

    @FunctionalInterface
    protected interface LongCountFunction {
        long count(Long id);
    }

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

    protected void validateDisplayOrders(Collection<Integer> orders, String errorDetail) {
        if (orders == null) return;

        Set<Integer> seen = new HashSet<>();
        for (Integer ord : orders) {
            int v = safeOrder(ord);
            if (!seen.add(v)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        "이미지 등록 중 오류가 발생했습니다.",
                        errorDetail
                );
            }
        }
    }

    protected void ensureImageOwnedBy(Long imageId, UUID expectedOwnerId, String errorDetail) {
        if (imageId == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "이미지 등록 중 오류가 발생했습니다.",
                    errorDetail
            );
        }
        if (expectedOwnerId == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "이미지 등록 중 오류가 발생했습니다.",
                    "IMAGE_OWNER_ID_REQUIRED"
            );
        }

        Image img = imageRepository.findById(imageId).orElse(null);
        if (img == null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.NOT_FOUND,
                    "이미지 등록 중 오류가 발생했습니다.",
                    errorDetail
            );
        }

        if (img.getUser() == null || img.getUser().getId() == null || !img.getUser().getId().equals(expectedOwnerId)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.FORBIDDEN,
                    "이미지 등록 중 오류가 발생했습니다.",
                    errorDetail
            );
        }
    }
}