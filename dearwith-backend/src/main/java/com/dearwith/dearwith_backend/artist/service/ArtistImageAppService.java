package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.AbstractSingleImageAppService;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ArtistImageAppService extends AbstractSingleImageAppService {

    public ArtistImageAppService(
            TmpImageGuard tmpImageGuard,
            ImageRepository imageRepository,
            AfterCommitExecutor afterCommitExecutor,
            AssetOps assetOps,
            ImageService imageService
    ) {
        super(tmpImageGuard, imageRepository, afterCommitExecutor, assetOps, imageService);
    }

    /**
     * 아티스트 프로필 최초 등록
     */
    @Transactional
    public void create(Artist artist, String tmpKey) {
        if (!hasTmp(tmpKey)) {
            return;
        }

        validateTmpKey(tmpKey);

        Image img = createTmpImage(tmpKey, artist.getUser());
        artist.setProfileImage(img);

        afterCommitExecutor.run(() -> {
            try {
                assetOps.commitSingleVariant(
                        AssetOps.CommitCommand.builder()
                                .imageId(img.getId())
                                .tmpKey(tmpKey)
                                .userId(artist.getUser() != null ? artist.getUser().getId() : null)
                                .preset(AssetVariantPreset.ARTIST)
                                .build()
                );
            } catch (Exception e) {
                log.error("[artist-image] commitSingleVariant (create) failed. imageId={}, tmpKey={}",
                        img.getId(), tmpKey, e);
            }
        });
    }

    /**
     * 아티스트 프로필 이미지 수정 (등록/변경/삭제 포함)
     */
    @Transactional
    public void update(Artist artist, String tmpKey) {

        Image before = artist.getProfileImage();

        // 삭제 케이스
        if (!hasTmp(tmpKey)) {
            if (before != null) {
                Long beforeId = before.getId();
                artist.setProfileImage(null);
                handleOrphan(beforeId);
            }
            return;
        }

        // 새로 교체
        validateTmpKey(tmpKey);

        Image img = createTmpImage(tmpKey, artist.getUser());
        artist.setProfileImage(img);

        afterCommitExecutor.run(() -> {
            try {
                assetOps.commitSingleVariant(
                        AssetOps.CommitCommand.builder()
                                .imageId(img.getId())
                                .tmpKey(tmpKey)
                                .userId(artist.getUser() != null ? artist.getUser().getId() : null)
                                .preset(AssetVariantPreset.ARTIST)
                                .build()
                );
            } catch (Exception e) {
                log.error("[artist-image] commitSingleVariant (update) failed. imageId={}, tmpKey={}",
                        img.getId(), tmpKey, e);
            }
        });

        // 이전 이미지 orphan 처리
        if (before != null) {
            handleOrphan(before.getId());
        }
    }

    /**
     * 아티스트 프로필 이미지 삭제
     */
    @Transactional
    public void delete(Artist artist) {
        Image before = artist.getProfileImage();
        if (before == null) return;

        artist.setProfileImage(null);
        handleOrphan(before.getId());
    }

    /**
     * 고아 이미지 처리: 어디에서도 안 쓰이면 soft delete
     * (필요에 따라 레포 메소드 이름 맞춰 수정)
     */
    private void handleOrphan(Long imageId) {
        if (imageId == null) return;

        boolean used = imageRepository.countArtistProfileUsages(imageId) > 0;

        if (!used) {
            imageService.softDeleteIfNotYet(imageId);
        }
    }
}