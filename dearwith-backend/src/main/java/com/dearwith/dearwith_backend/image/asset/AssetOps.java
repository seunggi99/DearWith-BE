package com.dearwith.dearwith_backend.image.asset;


import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import com.dearwith.dearwith_backend.external.aws.S3Waiter;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.*;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetOps {

    private final ImageRepository imageRepository;
    private final ImageAssetService imageAssetService;
    private final ImageVariantService imageVariantService;
    private final S3Waiter s3Waiter;
    private final AssetUrlService assetUrlService;
    private final EntityManager entityManager;

    /**
     * 기존 Image(row)를 받아 TMP → INLINE/COMMITTED로 승격하고,
     * S3 존재 대기 후 프리셋에 맞춰 파생본(variants)을 생성한다.
     * (트랜잭션 분리: REQUIRES_NEW)
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String commitExistingAndGenerateVariants(CommitCommand cmd) {
        // 1) S3 키 승격(tmp → inline)
        final String inlineKey = imageAssetService.promoteTmpToInline(cmd.getTmpKey());

        // 2) DB 갱신
        Image img = imageRepository.findById(cmd.getImageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이미지 없음: " + cmd.getImageId()));

        img.setS3Key(inlineKey);
        img.setStatus(ImageStatus.COMMITTED);
        if (img.getUser() == null && cmd.getUserId() != null) {
            img.setUser(entityManager.getReference(User.class, cmd.getUserId()));
        }
        img.setImageUrl(assetUrlService.generatePublicUrl(inlineKey));
        imageRepository.flush();

        // 3) S3 존재 대기
        s3Waiter.waitUntilExists(inlineKey);

        // 4) 파생본 생성
        imageVariantService.generateVariants(inlineKey, cmd.getPreset());

        log.debug("[asset-ops] committed imageId={}, key={}, preset={}", cmd.getImageId(), inlineKey, cmd.getPreset());
        return inlineKey;
    }

    @Value
    @Builder
    public static class CommitCommand {
        Long imageId;
        String tmpKey;
        UUID userId;
        AssetVariantPreset preset;
    }
}
