package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistGroupImageAppService {

    private final AssetOps assetOps;
    private final ImageRepository imageRepository;
    private final AfterCommitExecutor afterCommitExecutor;
    private final ImageService imageService;

    /**
     * 그룹 생성 시 이미지 등록 (단일 대표 이미지)
     */
    @Transactional
    public void create(ArtistGroup group, String tmpKey, User user) {
        if (tmpKey == null || tmpKey.isBlank()) {
            return; // 이미지 없이 생성 가능
        }

        // 1) TMP 이미지 row 생성
        Image img = new Image();
        img.setUser(user);
        img.setS3Key(tmpKey);
        img.setStatus(ImageStatus.TMP);
        imageRepository.save(img);

        group.setProfileImage(img);

        // 2) 커밋 후 TMP → inline + 단일 variant(main) 생성
        afterCommitExecutor.run(() -> {
            try {
                assetOps.commitSingleVariant(
                        AssetOps.CommitCommand.builder()
                                .imageId(img.getId())
                                .tmpKey(tmpKey)
                                .userId(user.getId())
                                .preset(AssetVariantPreset.ARTIST)   // 그룹도 동일 preset 사용
                                .build()
                );
            } catch (Exception e) {
                log.error("[artist-group-image] commitSingleVariant failed. imageId={}, tmpKey={}",
                        img.getId(), tmpKey, e);
            }
        });
    }

    /**
     * 그룹 이미지 수정
     * - tmpKey 비어있으면 → 삭제
     * - tmpKey 존재하면 → 새 이미지로 교체
     */
    @Transactional
    public void update(ArtistGroup group, String tmpKey, UUID userId) {

        Image before = group.getProfileImage();

        // 1) tmpKey가 비었으면 기존 이미지 제거
        if (tmpKey == null || tmpKey.isBlank()) {
            if (before != null) {
                Long beforeId = before.getId();
                group.setProfileImage(null);
                handleOrphan(beforeId);
            }
            return;
        }

        // 2) 신규 TMP 이미지 생성
        Image img = new Image();
        img.setUser(group.getUser());
        img.setS3Key(tmpKey);
        img.setStatus(ImageStatus.TMP);
        imageRepository.save(img);

        group.setProfileImage(img);

        // 3) 커밋 후 TMP → inline + single variant 생성
        afterCommitExecutor.run(() -> {
            try {
                assetOps.commitSingleVariant(
                        AssetOps.CommitCommand.builder()
                                .imageId(img.getId())
                                .tmpKey(tmpKey)
                                .userId(userId)
                                .preset(AssetVariantPreset.ARTIST)
                                .build()
                );
            } catch (Exception e) {
                log.error("[artist-group-image] commitSingleVariant(update) failed. imageId={}, tmpKey={}",
                        img.getId(), tmpKey, e);
            }
        });

        // 4) 기존 이미지 orphan 처리
        if (before != null) {
            handleOrphan(before.getId());
        }
    }

    /**
     * 그룹 이미지 삭제
     */
    @Transactional
    public void delete(ArtistGroup group) {
        Image before = group.getProfileImage();
        if (before == null) return;

        group.setProfileImage(null);
        handleOrphan(before.getId());
    }

    /**
     * 고아 이미지 처리
     */
    private void handleOrphan(Long imageId) {
        if (imageId == null) return;

        boolean used = imageRepository.countArtistGroupProfileUsages(imageId) > 0;

        if (!used) {
            imageService.softDeleteIfNotYet(imageId);
        }
    }
}