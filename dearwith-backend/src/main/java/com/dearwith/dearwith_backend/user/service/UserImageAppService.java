package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserImageAppService {
    private final AssetOps assetOps;
    private final ImageRepository imageRepository;
    private final AfterCommitExecutor afterCommitExecutor;
    private final ImageService imageService;

    /**
     * 유저 프로필 최초 등록
     */
    @Transactional
    public void create(User user, String tmpKey) {
        if (tmpKey == null || tmpKey.isBlank()) {
            return;
        }

        Image img = new Image();
        img.setUser(user);
        img.setS3Key(tmpKey);
        img.setStatus(ImageStatus.TMP);
        imageRepository.save(img);

        user.setProfileImage(img);

        afterCommitExecutor.run(() -> {
            try {
                assetOps.commitSingleVariant(
                        AssetOps.CommitCommand.builder()
                                .imageId(img.getId())
                                .tmpKey(tmpKey)
                                .userId(user.getId())
                                .preset(AssetVariantPreset.USER)
                                .build()
                );
            } catch (Exception e) {
                log.error("[user-image] commitUserSingleVariant (create) failed. imageId={}, tmpKey={}",
                        img.getId(), tmpKey, e);
            }
        });
    }

    /**
     * 유저 프로필 이미지 수정 (등록/변경/삭제 포함)
     * - tmpKey가 null/blank 이면 → 기존 이미지 삭제
     * - tmpKey가 값이 있으면 → 새 이미지로 교체
     */
    @Transactional
    public void update(User user, String tmpKey) {

        Image before = user.getProfileImage();

        // 1) tmpKey 없으면: 기존 이미지 제거
        if (tmpKey == null || tmpKey.isBlank()) {
            if (before != null) {
                Long beforeId = before.getId();
                user.setProfileImage(null);
                handleOrphan(beforeId);
            }
            return;
        }

        // 2) 신규 TMP 이미지 row 생성
        Image img = new Image();
        img.setUser(user);
        img.setS3Key(tmpKey);
        img.setStatus(ImageStatus.TMP);
        imageRepository.save(img);

        user.setProfileImage(img);

        // 3) 커밋 후 TMP → inline + variant 생성
        afterCommitExecutor.run(() -> {
            try {
                assetOps.commitSingleVariant(
                        AssetOps.CommitCommand.builder()
                                .imageId(img.getId())
                                .tmpKey(tmpKey)
                                .userId(user.getId())
                                .preset(AssetVariantPreset.USER)
                                .build()
                );
            } catch (Exception e) {
                log.error("[user-image] commitUserSingleVariant (update) failed. imageId={}, tmpKey={}",
                        img.getId(), tmpKey, e);
            }
        });

        // 4) 기존 이미지 orphan 처리
        if (before != null) {
            handleOrphan(before.getId());
        }
    }

    /**
     * 유저 프로필 이미지 삭제
     */
    @Transactional
    public void delete(User user) {
        Image before = user.getProfileImage();
        if (before == null) return;

        user.setProfileImage(null);
        handleOrphan(before.getId());
    }

    /**
     * 고아 이미지 처리: 어디에서도 안 쓰이면 soft delete
     */
    private void handleOrphan(Long imageId) {
        if (imageId == null) return;

        boolean used = imageRepository.countUserProfileUsages(imageId) > 0;

        if (!used) {
            imageService.softDeleteIfNotYet(imageId);
        }
    }
}
