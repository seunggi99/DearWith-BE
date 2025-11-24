package com.dearwith.dearwith_backend.image.service;


import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.entity.EventImageMapping;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageAttachmentService {

    private final ImageAssetService imageAssetService;
    private final ImageService imageService;

    /**
     * 아티스트 프로필 이미지 교체
     */
    @Transactional
    public Image setArtistProfileImage(Artist artist, String tmpKey, UUID userId) {
        if (tmpKey == null || tmpKey.isBlank()) return null;

        if (!tmpKey.startsWith("tmp/")) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_TMP_KEY,
                    ErrorCode.INVALID_TMP_KEY.getMessage(),
                    "ARTIST_PROFILE_TMPKEY_INVALID"
            );
        }

        String inlineKey = commitTmpToInline(tmpKey);
        Image image = imageService.registerCommittedImage(inlineKey, userId);
        artist.setProfileImage(image);
        return image;
    }

    /**
     * tmpKey를 inline 경로로 승격
     */
    private String commitTmpToInline(String tmpKey) {
        try {
            return imageAssetService.promoteTmpToInline(tmpKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.withAll(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "COMMIT_TMP_TO_INLINE_FAILED",
                    "commitTmpToInline failed: " + e.getMessage(),
                    e
            );
        }
    }
}