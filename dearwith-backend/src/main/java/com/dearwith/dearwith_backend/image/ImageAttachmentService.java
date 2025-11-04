package com.dearwith.dearwith_backend.image;


import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.entity.EventImageMapping;
import com.dearwith.dearwith_backend.external.aws.S3UploadService;
import com.dearwith.dearwith_backend.review.entity.Review;
import com.dearwith.dearwith_backend.review.entity.ReviewImageMapping;
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

    private final S3UploadService s3UploadService;
    private final ImageService imageService;

    @Transactional
    public void setReviewImages(Review review, List<ImageAttachmentRequest> images, UUID userId) {

        review.clearImages();


        for (ImageAttachmentRequest dto : images) {
            String tmpKey = dto.tmpKey();
            if (tmpKey == null || !tmpKey.startsWith("tmp/")) {
                throw new BusinessException(ErrorCode.INVALID_TMP_KEY, "tmpKey=" + tmpKey);
            }

            // tmp -> inline 승격 + 이미지 엔티티 등록 + (리뷰용) 버전 생성
            Image image = imageService.registerCommittedImageWithVariants(
                    s3UploadService.promoteTmpToInline(tmpKey),
                    userId,
                    ReviewVariantPresets.reviewImageSet() // 리뷰용 프리셋
            );

            ReviewImageMapping mapping = ReviewImageMapping.builder()
                    .image(image)
                    .displayOrder(dto.displayOrder() != null ? dto.displayOrder() : 0)
                    .build();

            review.addImageMapping(mapping); // 내부에서 review/eventId 세팅 + 리스트 add
        }
    }

//    @Transactional
//    public void setReviewImages(Review review, List<ImageAttachmentRequest> images, UUID userId) {
//        attachImages(
//                images,
//                userId,
//                () -> false,
//                img -> {},
//                (img, order) -> review.addImageMapping(
//                        ReviewImageMapping.builder()
//                                .review(review)
//                                .image(img)
//                                .displayOrder(order)
//                                .build()
//                )
//        );
//    }

    @Transactional
    public void setEventImages(Event event, List<ImageAttachmentRequest> images, UUID userId) {
        attachImages(
                images,
                userId,
                () -> event.getCoverImage() != null,
                event::setCoverImage,
                (img, order) -> event.addImageMapping(
                        EventImageMapping.builder()
                                .event(event)
                                .image(img)
                                .displayOrder(order)
                                .build()
                )
        );
    }
    @Transactional
    public Image setArtistProfileImage(Artist artist, String tmpKey, UUID userId) {
        if (tmpKey == null || tmpKey.isBlank()) return null;
        if (!tmpKey.startsWith("tmp/")) {
            throw new BusinessException(ErrorCode.INVALID_TMP_KEY, "tmpKey=" + tmpKey);
        }

        final String inlineKey = commitTmpToInline(tmpKey);
        Image image = imageService.registerCommittedImage(inlineKey, userId);
        artist.setProfileImage(image);
        return image;
    }

    @Transactional
    public void attachImages(List<ImageAttachmentRequest> images,
                             UUID userId,
                             Supplier<Boolean> hasCover,
                             Consumer<Image> setCover,
                             BiConsumer<Image, Integer> addMapping) {
        if (images == null || images.isEmpty()) return;

        // 1) tmpKey 검증 + 중복 체크
        Set<String> dupCheck = new HashSet<>();
        for (ImageAttachmentRequest dto : images) {
            String tmpKey = dto.tmpKey();
            if (tmpKey == null || !tmpKey.startsWith("tmp/")) {
                throw new BusinessException(ErrorCode.INVALID_TMP_KEY, "tmpKey=" + tmpKey);
            }
            if (!dupCheck.add(tmpKey)) {
                throw new BusinessException(ErrorCode.DUPLICATE_IMAGE_KEY, tmpKey);
            }
        }

        boolean coverAlready = hasCover.get();
        Image firstImageForCover = null;

        // 2) 각 이미지 처리
        for (ImageAttachmentRequest dto : images) {
            String inlineKey = commitTmpToInline(dto.tmpKey());

            // 기존에 잘 동작하던 로직 그대로 사용
            Image image = imageService.registerCommittedImage(inlineKey, userId);

            // 도메인 매핑 추가
            addMapping.accept(image, dto.displayOrder());

            if (!coverAlready && firstImageForCover == null) {
                firstImageForCover = image;
            }
        }

        // 3) 커버 자동 지정
        if (!coverAlready && firstImageForCover != null) {
            setCover.accept(firstImageForCover);
        }
    }

    private String commitTmpToInline(String tmpKey) {
        try {
            return s3UploadService.promoteTmpToInline(tmpKey);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("size")) {
                throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE, msg);
            } else if (msg.contains("content type")) {
                throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE, msg);
            } else {
                throw new BusinessException(ErrorCode.S3_COMMIT_FAILED, msg);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.S3_COMMIT_FAILED, e.getMessage());
        }
    }
}