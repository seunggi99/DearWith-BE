package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.AbstractSingleImageAppService;
import com.dearwith.dearwith_backend.image.service.ImageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArtistGroupImageAppService extends AbstractSingleImageAppService {

    private static final String LOG_TAG = "artist-group-image";

    public ArtistGroupImageAppService(
            TmpImageGuard tmpImageGuard,
            ImageRepository imageRepository,
            AfterCommitExecutor afterCommitExecutor,
            AssetOps assetOps,
            ImageService imageService
    ) {
        super(tmpImageGuard, imageRepository, afterCommitExecutor, assetOps, imageService);
    }

    @Transactional
    public void create(ArtistGroup group, String tmpKey) {
        if (!hasTmp(tmpKey)) return;

        validateTmpKey(tmpKey);

        Image img = createTmpImage(tmpKey, group.getUser());
        group.setProfileImage(img);

        commitSingleAfterTransaction(
                LOG_TAG,
                img.getId(),
                tmpKey,
                group.getUser() != null ? group.getUser().getId() : null,
                AssetVariantPreset.ARTIST
        );
    }

    @Transactional
    public void update(ArtistGroup group, String tmpKey) {
        Image before = group.getProfileImage();

        // 삭제
        if (!hasTmp(tmpKey)) {
            if (before != null) {
                Long beforeId = before.getId();
                group.setProfileImage(null);
                handleOrphans(List.of(beforeId), imageRepository::countArtistGroupProfileUsages);
            }
            return;
        }

        // 교체
        validateTmpKey(tmpKey);

        Image img = createTmpImage(tmpKey, group.getUser());
        group.setProfileImage(img);

        commitSingleAfterTransaction(
                LOG_TAG,
                img.getId(),
                tmpKey,
                group.getUser() != null ? group.getUser().getId() : null,
                AssetVariantPreset.ARTIST
        );

        if (before != null) {
            handleOrphans(List.of(before.getId()), imageRepository::countArtistGroupProfileUsages);
        }
    }

    @Transactional
    public void delete(ArtistGroup group) {
        Image before = group.getProfileImage();
        if (before == null) return;

        group.setProfileImage(null);
        handleOrphans(List.of(before.getId()), imageRepository::countArtistGroupProfileUsages);
    }
}