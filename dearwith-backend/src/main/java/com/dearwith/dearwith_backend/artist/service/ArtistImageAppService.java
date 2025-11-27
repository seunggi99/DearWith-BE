package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.entity.Artist;
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
public class ArtistImageAppService {

    private final AssetOps assetOps;
    private final ImageRepository imageRepository;
    private final AfterCommitExecutor afterCommitExecutor;
    private final ImageService imageService;

    /**
     * 아티스트 생성 시 이미지 등록 (단일 대표 이미지)
     */
    @Transactional
    public void create(Artist artist, String tmpKey, User user) {
        if (tmpKey == null || tmpKey.isBlank()) {
            return; // 이미지 없이 생성 가능
        }

        // 1) 트랜잭션 안: TMP 이미지 row 생성
        Image img = new Image();
        img.setUser(user);
        img.setS3Key(tmpKey);          // 일단 TMP key
        img.setStatus(ImageStatus.TMP);
        imageRepository.save(img);

        // 아티스트에 프로필 이미지 연결
        artist.setProfileImage(img);

        // 2) 커밋 후: TMP → inline 승격 + 단일 main.webp 생성 + imageUrl 세팅
        afterCommitExecutor.run(() -> {
            try {
                assetOps.commitSingleVariant(
                        AssetOps.CommitCommand.builder()
                                .imageId(img.getId())
                                .tmpKey(tmpKey)
                                .userId(user.getId())
                                .preset(AssetVariantPreset.ARTIST)
                                .build()
                );
            } catch (Exception e) {
                log.error("[artist-image] commitArtistSingleVariant failed. imageId={}, tmpKey={}",
                        img.getId(), tmpKey, e);
            }
        });
    }

    /**
     * 아티스트 이미지 수정 (단일 이미지 갱신)
     * - tmpKey가 null/blank 이면 → 기존 이미지 삭제
     * - tmpKey가 값이 있으면 → 새 이미지로 교체
     */
    @Transactional
    public void update(Artist artist, String tmpKey, UUID userId) {

        Image before = artist.getProfileImage();

        // 1) tmpKey가 null 또는 공백이면: 기존 이미지 제거
        if (tmpKey == null || tmpKey.isBlank()) {
            if (before != null) {
                Long beforeId = before.getId();
                artist.setProfileImage(null);
                handleOrphan(beforeId);
            }
            return;
        }

        // 2) 신규 TMP 이미지 row 생성
        Image img = new Image();
        img.setUser(artist.getUser());
        img.setS3Key(tmpKey);
        img.setStatus(ImageStatus.TMP);
        imageRepository.save(img);

        artist.setProfileImage(img);

        // 3) 커밋 후: TMP → inline + 단일 main.webp 생성
        afterCommitExecutor.run(() -> {
            try {
                assetOps.commitSingleVariant(
                        AssetOps.CommitCommand.builder()
                                .imageId(img.getId())
                                .tmpKey(tmpKey)
                                .userId(userId)
                                .build()
                );
            } catch (Exception e) {
                log.error("[artist-image] commitArtistSingleVariant (update) failed. imageId={}, tmpKey={}",
                        img.getId(), tmpKey, e);
            }
        });

        // 4) 기존 이미지 orphan 처리
        if (before != null) {
            handleOrphan(before.getId());
        }
    }

    /**
     * 아티스트 이미지 삭제
     */
    @Transactional
    public void delete(Artist artist) {
        Image before = artist.getProfileImage();
        if (before == null) return;

        artist.setProfileImage(null);
        handleOrphan(before.getId());
    }

    /**
     * 고아 이미지 처리: 어디에도 안 쓰이면 soft delete
     */
    private void handleOrphan(Long imageId) {
        if (imageId == null) return;

        boolean used = imageRepository.countArtistProfileUsages(imageId) > 0;

        if (!used) {
            imageService.softDeleteIfNotYet(imageId);
        }
    }
}