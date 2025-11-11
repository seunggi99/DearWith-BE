package com.dearwith.dearwith_backend.image.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final ImageAssetService imageAssetService;
    private final AssetUrlService assetUrlService;
    private final UserRepository userRepository;

    @Transactional
    public Image registerCommittedImage(String finalKey, UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        String url = assetUrlService.generatePublicUrl(finalKey);

        Image image = Image.builder()
                .s3Key(finalKey)
                .imageUrl(url)
                .status(ImageStatus.COMMITTED)
                .user(user)
                .build();

        return imageRepository.save(image);
    }

    @Transactional
    public void markDeleted(Image image) {
        if (image.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이미 삭제된 이미지입니다. imageId=" + image.getId());
        }
        imageAssetService.moveOriginalAndDerivedToTrash(image.getS3Key());
        image.setDeletedAt(LocalDateTime.now());
        imageRepository.save(image);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String promoteAndCommit(Long imageId, String tmpKey) {
        String inlineKey = imageAssetService.promoteTmpToInline(tmpKey);

        Image img = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이미지 없음: " + imageId));
        img.setS3Key(inlineKey);
        img.setStatus(ImageStatus.COMMITTED);
        img.setImageUrl(assetUrlService.generatePublicUrl(inlineKey));

        imageRepository.flush();

        return inlineKey;
    }

    @Transactional
    public void softDeleteIfNotYet(Long imageId) {
        Image img = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이미지 없음: " + imageId));
        if (img.getDeletedAt() == null) {
            markDeleted(img);
        }
    }
}
