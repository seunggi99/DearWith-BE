package com.dearwith.dearwith_backend.image.service;

import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.AbstractImageSupport;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.image.asset.AssetOps;

import java.util.UUID;
import java.util.function.Consumer;

public abstract class AbstractSingleImageAppService extends AbstractImageSupport {

    protected AbstractSingleImageAppService(
            TmpImageGuard tmpImageGuard,
            ImageRepository imageRepository,
            AfterCommitExecutor afterCommitExecutor,
            AssetOps assetOps,
            ImageService imageService
    ) {
        super(tmpImageGuard, imageRepository, afterCommitExecutor, assetOps, imageService);
    }

    /**
     * 단일 이미지 갱신 공통 패턴
     *
     * @param before        기존 이미지 (없으면 null)
     * @param tmpKey        새 tmpKey (null/blank 이면 삭제)
     * @param owner         이미지 소유자 (User) – null 허용 X
     * @param userId        커밋 시 userId
     * @param preset        프리셋 (null 금지)
     * @param domainSetter  도메인에 Image 세터 (예: artist::setProfileImage)
     * @param orphanHandler 이전 이미지 orphan 처리 람다 (before != null 인 경우에만 호출)
     */
    protected void updateSingleImage(
            String logTag,
            Image before,
            String tmpKey,
            User owner,
            UUID userId,
            AssetVariantPreset preset,
            Consumer<Image> domainSetter,
            Runnable orphanHandler
    ) {
        // 삭제 케이스 (tmpKey 비어있음)
        if (!hasTmp(tmpKey)) {
            domainSetter.accept(null);
            if (before != null && orphanHandler != null) {
                orphanHandler.run();
            }
            return;
        }

        // 새 tmpKey 검증
        validateTmpKey(tmpKey);

        // 신규 TMP 이미지 생성
        Image img = createTmpImage(tmpKey, owner);
        domainSetter.accept(img);

        // after-commit 커밋
        commitAfterTransaction(logTag,img.getId(), tmpKey, userId, preset);

        // 기존 orphan 처리
        if (before != null && orphanHandler != null) {
            orphanHandler.run();
        }
    }
}