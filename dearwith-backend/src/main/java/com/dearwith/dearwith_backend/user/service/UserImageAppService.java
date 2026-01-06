package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.AbstractSingleImageAppService;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserImageAppService extends AbstractSingleImageAppService {

    private static final String LOG_TAG = "user-image";

    public UserImageAppService(
            TmpImageGuard tmpImageGuard,
            ImageRepository imageRepository,
            AfterCommitExecutor afterCommitExecutor,
            AssetOps assetOps,
            ImageService imageService
    ) {
        super(tmpImageGuard, imageRepository, afterCommitExecutor, assetOps, imageService);
    }

    @Transactional
    public void create(User user, String tmpKey) {
        if (!hasTmp(tmpKey)) return;

        validateTmpKey(tmpKey);

        Image img = createTmpImage(tmpKey, user);
        user.setProfileImage(img);

        commitSingleAfterTransaction(
                LOG_TAG,
                img.getId(),
                tmpKey,
                user.getId(),
                AssetVariantPreset.USER
        );
    }

    @Transactional
    public void update(User user, String tmpKey) {
        Image before = user.getProfileImage();

        // 삭제
        if (!hasTmp(tmpKey)) {
            if (before != null) {
                Long beforeId = before.getId();
                user.setProfileImage(null);
                handleOrphans(List.of(beforeId), imageRepository::countUserProfileUsages);
            }
            return;
        }

        // 교체
        validateTmpKey(tmpKey);

        Image img = createTmpImage(tmpKey, user);
        user.setProfileImage(img);

        commitSingleAfterTransaction(
                LOG_TAG,
                img.getId(),
                tmpKey,
                user.getId(),
                AssetVariantPreset.USER
        );

        if (before != null) {
            handleOrphans(List.of(before.getId()), imageRepository::countUserProfileUsages);
        }
    }

    @Transactional
    public void delete(User user) {
        Image before = user.getProfileImage();
        if (before == null) return;

        user.setProfileImage(null);
        handleOrphans(List.of(before.getId()), imageRepository::countUserProfileUsages);
    }
}