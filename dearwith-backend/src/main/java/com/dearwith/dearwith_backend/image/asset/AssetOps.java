package com.dearwith.dearwith_backend.image.asset;


import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import com.dearwith.dearwith_backend.external.aws.AwsS3ClientAdapter;
import com.dearwith.dearwith_backend.external.aws.S3Waiter;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.enums.ImageProcessStatus;
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.*;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
    private final ImageVariantService imageVariantService;
    private final S3Waiter s3Waiter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateVariantsAndMarkStatus(Long imageId, AssetVariantPreset preset) {
        Image img = imageRepository.findById(imageId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "이미지를 찾을 수 없습니다.",
                        "IMAGE_NOT_FOUND:" + imageId
                ));

        if (img.getDeletedAt() != null) return;
        if (img.getStatus() != ImageStatus.COMMITTED) return;

        String originalKey = img.getS3Key();

        try {
            // 원본 존재 대기
            s3Waiter.waitUntilExists(originalKey);

            // variants 생성
            imageVariantService.generateVariants(originalKey, preset);

            // 성공 → READY
            img.setProcessStatus(ImageProcessStatus.READY);
            imageRepository.flush();

            log.info("[asset-ops] variants done imageId={}, key={}, preset={}", imageId, originalKey, preset);

        } catch (Throwable t) {
            log.error("[asset-ops] variants failed imageId={}, key={}, preset={}", imageId, originalKey, preset, t);

            //  실패 → FAILED
            img.setProcessStatus(ImageProcessStatus.FAILED);
            imageRepository.flush();
        }
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
