package com.dearwith.dearwith_backend.image.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.service.UserReader;
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
    private final UserReader userReader;

    @Transactional
    public Image registerCommittedImage(String finalKey, UUID userId) {
        User user = userReader.getUser(userId);

        Image image = Image.builder()
                .s3Key(finalKey)
                .status(ImageStatus.COMMITTED)
                .user(user)
                .build();

        return imageRepository.save(image);
    }

    @Transactional
    public void markDeleted(Image image) {
        if (image.getDeletedAt() != null) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.IMAGE_ALREADY_DELETED,
                    ErrorCode.IMAGE_ALREADY_DELETED.getMessage(),
                    "IMAGE_ALREADY_DELETED:" + image.getId()
            );
        }

        imageAssetService.moveOriginalAndDerivedToTrash(image.getS3Key());
        image.setDeletedAt(LocalDateTime.now());
        imageRepository.save(image);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String promoteAndCommit(Long imageId, String tmpKey) {
        String inlineKey = imageAssetService.promoteTmpToInline(tmpKey);

        Image img = imageRepository.findById(imageId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "존재하지 않는 이미지입니다.",
                        "IMAGE_NOT_FOUND:" + imageId
                ));

        img.setS3Key(inlineKey);
        img.setStatus(ImageStatus.COMMITTED);

        imageRepository.flush();
        return inlineKey;
    }

    @Transactional
    public void softDeleteIfNotYet(Long imageId) {
        Image img = imageRepository.findById(imageId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.NOT_FOUND,
                        "존재하지 않는 이미지입니다.",
                        "IMAGE_NOT_FOUND:" + imageId
                ));

        if (img.getDeletedAt() == null) {
            markDeleted(img);
        }
    }
}
