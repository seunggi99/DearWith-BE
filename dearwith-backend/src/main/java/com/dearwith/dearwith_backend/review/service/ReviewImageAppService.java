package com.dearwith.dearwith_backend.review.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentUpdateRequestDto;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.AbstractImageSupport;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.review.entity.Review;
import com.dearwith.dearwith_backend.review.entity.ReviewImageMapping;
import com.dearwith.dearwith_backend.review.enums.ReviewStatus;
import com.dearwith.dearwith_backend.review.repository.ReviewImageMappingRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReviewImageAppService extends AbstractImageSupport {

    private static final String LOG_TAG = "review-image";
    private final ReviewImageMappingRepository reviewImageMappingRepository;

    public ReviewImageAppService(
            TmpImageGuard tmpImageGuard,
            ImageRepository imageRepository,
            AfterCommitExecutor afterCommitExecutor,
            AssetOps assetOps,
            ImageService imageService,
            ReviewImageMappingRepository reviewImageMappingRepository
    ) {
        super(tmpImageGuard, imageRepository, afterCommitExecutor, assetOps, imageService);
        this.reviewImageMappingRepository = reviewImageMappingRepository;
    }

    @Transactional
    public void create(Review review, List<ImageAttachmentRequestDto> images, User user) {
        if (images == null || images.isEmpty()) return;

        validateTmpKeys(images.stream().map(ImageAttachmentRequestDto::tmpKey).toList());
        requireTmpPrefixAll(images.stream().map(ImageAttachmentRequestDto::tmpKey).toList(),
                "REVIEW_IMAGE_TMPKEY_INVALID");

        validateDisplayOrders(
                images.stream().map(ImageAttachmentRequestDto::displayOrder).toList(),
                "REVIEW_IMAGE_DISPLAY_ORDER_DUPLICATED"
        );

        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        for (ImageAttachmentRequestDto dto : images) {
            String tmpKey = dto.tmpKey();
            requireTmpPrefix(tmpKey, "REVIEW_IMAGE_TMPKEY_INVALID");

            Image img = createTmpImage(tmpKey, user);

            ReviewImageMapping mapping = ReviewImageMapping.builder()
                    .image(img)
                    .review(review)
                    .eventId(review.getEvent().getId())
                    .displayOrder(safeOrder(dto.displayOrder()))
                    .reviewStatus(ReviewStatus.VISIBLE)
                    .build();
            review.addImageMapping(mapping);

            created.add(new NewImage(img.getId(), tmpKey));
        }

        for (NewImage ni : created) {
            commitAfterTransaction(LOG_TAG, ni.id(), ni.tmpKey(), user.getId(), AssetVariantPreset.REVIEW);
        }
    }

    @Transactional
    public void update(Review review, List<ImageAttachmentUpdateRequestDto> reqs, UUID userId) {
        if (reqs == null) return;

        if (reqs.isEmpty()) {
            deleteAll(review.getId());
            return;
        }

        validateTmpKeys(reqs.stream().map(ImageAttachmentUpdateRequestDto::tmpKey).toList());

        validateDisplayOrders(
                reqs.stream().map(ImageAttachmentUpdateRequestDto::displayOrder).toList(),
                "REVIEW_IMAGE_DISPLAY_ORDER_DUPLICATED"
        );

        // 요청 형식 검증 + 기존 id 존재 검증 + tmp/ prefix 검증
        for (var r : reqs) {
            boolean hasId  = r.id() != null;
            boolean hasTmp = hasTmp(r.tmpKey());

            if (hasId == hasTmp) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        "이미지 등록 중 오류가 발생했습니다.",
                        "REVIEW_IMAGE_ID_OR_TMPKEY_XOR_REQUIRED"
                );
            }
            if (hasId) {
                ensureImageOwnedBy(r.id(), review.getUser().getId(), "REVIEW_IMAGE_ID_NOT_OWNED");
            } else {
                requireTmpPrefix(r.tmpKey(), "REVIEW_IMAGE_TMPKEY_INVALID");
            }
        }

        // before snapshot
        List<ReviewImageMapping> beforeMappings =
                reviewImageMappingRepository.findByReviewId(review.getId());
        List<Long> beforeIds = beforeMappings.stream()
                .map(m -> m.getImage().getId())
                .toList();

        reviewImageMappingRepository.deleteByReviewId(review.getId());

        Map<Long, Integer> orderById = new HashMap<>();
        List<Long> finalIds = new ArrayList<>();

        // 기존 유지(id)
        for (var r : reqs) {
            if (r.id() != null) {
                Long imageId = r.id();
                finalIds.add(imageId);
                orderById.put(imageId, safeOrder(r.displayOrder()));
            }
        }

        // 신규 추가(tmpKey)
        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        for (var r : reqs) {
            if (hasTmp(r.tmpKey())) {
                String tmpKey = r.tmpKey();
                requireTmpPrefix(tmpKey, "REVIEW_IMAGE_TMPKEY_INVALID");

                Image img = createTmpImage(tmpKey, review.getUser()); // 작성자 고정

                Long id = img.getId();
                finalIds.add(id);
                orderById.put(id, safeOrder(r.displayOrder()));

                created.add(new NewImage(id, tmpKey));
            }
        }

        // 매핑 재생성
        for (Long imageId : finalIds) {
            ReviewImageMapping m = ReviewImageMapping.builder()
                    .review(review)
                    .eventId(review.getEvent().getId())
                    .image(imageRepository.getReferenceById(imageId))
                    .displayOrder(orderById.get(imageId))
                    .reviewStatus(ReviewStatus.VISIBLE)
                    .build();
            reviewImageMappingRepository.save(m);
        }

        // after-commit: TMP → inline + variants
        for (NewImage ni : created) {
            commitAfterTransaction(LOG_TAG, ni.id(), ni.tmpKey(), userId, AssetVariantPreset.REVIEW);
        }

        // orphan 처리
        Set<Long> finalSet = new HashSet<>(finalIds);
        List<Long> removed = beforeIds.stream()
                .filter(id -> !finalSet.contains(id))
                .toList();

        handleOrphans(removed, reviewImageMappingRepository::countUsages);
    }

    @Transactional
    public void deleteAll(Long reviewId) {
        List<Long> before = reviewImageMappingRepository.findByReviewId(reviewId)
                .stream()
                .map(m -> m.getImage().getId())
                .toList();

        reviewImageMappingRepository.deleteByReviewId(reviewId);
        handleOrphans(before, reviewImageMappingRepository::countUsages);
    }

    @Transactional
    public void delete(Long reviewId) {
        List<Long> before = reviewImageMappingRepository.findByReviewId(reviewId)
                .stream()
                .map(m -> m.getImage().getId())
                .toList();

        reviewImageMappingRepository.deleteByReviewId(reviewId);
        handleOrphans(before, reviewImageMappingRepository::countUsages);
    }
}