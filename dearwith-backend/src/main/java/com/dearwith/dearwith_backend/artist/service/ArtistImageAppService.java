package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.entity.Artist;
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
public class ArtistImageAppService extends AbstractSingleImageAppService {

    private static final String LOG_TAG = "artist-image";

    public ArtistImageAppService(
            TmpImageGuard tmpImageGuard,
            ImageRepository imageRepository,
            AfterCommitExecutor afterCommitExecutor,
            AssetOps assetOps,
            ImageService imageService
    ) {
        super(tmpImageGuard, imageRepository, afterCommitExecutor, assetOps, imageService);
    }

    @Transactional
    public void create(Artist artist, String tmpKey) {
        if (!hasTmp(tmpKey)) return;

        validateTmpKey(tmpKey);

        Image img = createTmpImage(tmpKey, artist.getUser());
        artist.setProfileImage(img);

        commitSingleAfterTransaction(
                LOG_TAG,
                img.getId(),
                tmpKey,
                artist.getUser() != null ? artist.getUser().getId() : null,
                AssetVariantPreset.ARTIST
        );
    }

    @Transactional
    public void update(Artist artist, String tmpKey) {
        Image before = artist.getProfileImage();

        // 삭제
        if (!hasTmp(tmpKey)) {
            if (before != null) {
                Long beforeId = before.getId();
                artist.setProfileImage(null);
                handleOrphans(List.of(beforeId), imageRepository::countArtistProfileUsages);
            }
            return;
        }

        // 교체
        validateTmpKey(tmpKey);

        Image img = createTmpImage(tmpKey, artist.getUser());
        artist.setProfileImage(img);

        commitSingleAfterTransaction(
                LOG_TAG,
                img.getId(),
                tmpKey,
                artist.getUser() != null ? artist.getUser().getId() : null,
                AssetVariantPreset.ARTIST
        );

        if (before != null) {
            handleOrphans(List.of(before.getId()), imageRepository::countArtistProfileUsages);
        }
    }

    @Transactional
    public void delete(Artist artist) {
        Image before = artist.getProfileImage();
        if (before == null) return;

        artist.setProfileImage(null);
        handleOrphans(List.of(before.getId()), imageRepository::countArtistProfileUsages);
    }
}