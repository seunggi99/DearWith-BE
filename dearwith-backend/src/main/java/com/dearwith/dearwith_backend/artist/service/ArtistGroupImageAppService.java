package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.AbstractSingleImageAppService;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ArtistGroupImageAppService extends AbstractSingleImageAppService {

    public ArtistGroupImageAppService(
            TmpImageGuard tmpImageGuard,
            ImageRepository imageRepository,
            AfterCommitExecutor afterCommitExecutor,
            AssetOps assetOps,
            ImageService imageService
    ) {
        super(tmpImageGuard, imageRepository, afterCommitExecutor, assetOps, imageService);
    }

    /**
     * 그룹 프로필 최초 등록
     */
    @Transactional
    public void create(ArtistGroup group, String tmpKey) {
        if (!hasTmp(tmpKey)) {
            return;
        }

        validateTmpKey(tmpKey);

        Image img = createTmpImage(tmpKey, group.getUser());
        group.setProfileImage(img);

        afterCommitExecutor.run(() -> {
            try {
                assetOps.commitSingleVariant(
                        AssetOps.CommitCommand.builder()
                                .imageId(img.getId())
                                .tmpKey(tmpKey)
                                .userId(group.getUser() != null ? group.getUser().getId() : null)
                                .preset(AssetVariantPreset.ARTIST)
                                .build()
                );
            } catch (Exception e) {
                log.error("[artist-group-image] commitSingleVariant (create) failed. imageId={}, tmpKey={}",
                        img.getId(), tmpKey, e);
            }
        });
    }

    /**
     * 그룹 프로필 수정 (등록/변경/삭제 포함)
     */
    @Transactional
    public void update(ArtistGroup group, String tmpKey) {

        Image before = group.getProfileImage();

        // 삭제 케이스
        if (!hasTmp(tmpKey)) {
            if (before != null) {
                Long beforeId = before.getId();
                group.setProfileImage(null);
                handleOrphan(beforeId);
            }
            return;
        }

        validateTmpKey(tmpKey);

        Image img = createTmpImage(tmpKey, group.getUser());
        group.setProfileImage(img);

        afterCommitExecutor.run(() -> {
            try {
                assetOps.commitSingleVariant(
                        AssetOps.CommitCommand.builder()
                                .imageId(img.getId())
                                .tmpKey(tmpKey)
                                .userId(group.getUser() != null ? group.getUser().getId() : null)
                                .preset(AssetVariantPreset.ARTIST)
                                .build()
                );
            } catch (Exception e) {
                log.error("[artist-group-image] commitSingleVariant (update) failed. imageId={}, tmpKey={}",
                        img.getId(), tmpKey, e);
            }
        });

        if (before != null) {
            handleOrphan(before.getId());
        }
    }

    /**
     * 그룹 프로필 이미지 삭제
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