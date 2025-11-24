package com.dearwith.dearwith_backend.review.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.external.aws.S3Waiter;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentUpdateRequestDto;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.image.service.ImageVariantService;
import com.dearwith.dearwith_backend.review.entity.Review;
import com.dearwith.dearwith_backend.review.entity.ReviewImageMapping;
import com.dearwith.dearwith_backend.review.enums.ReviewStatus;
import com.dearwith.dearwith_backend.review.repository.ReviewImageMappingRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewImageAppService {

    private final AssetOps assetOps;
    private final ImageRepository imageRepository;
    private final AfterCommitExecutor afterCommitExecutor;
    private final ImageService imageService;
    private final S3Waiter s3Waiter;
    private final ReviewImageMappingRepository reviewImageMappingRepository;
    private final ImageVariantService imageVariantService;

    /**
     * 리뷰 생성 시 이미지 등록
     */
    @Transactional
    public void create(Review review, List<ImageAttachmentRequestDto> images, User user) {
        if (images == null || images.isEmpty()) {
            return;
        }

        Set<Integer> seen = new HashSet<>();
        for (ImageAttachmentRequestDto dto : images) {
            Integer ord = dto.displayOrder() == null ? 0 : dto.displayOrder();
            if (!seen.add(ord)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        ErrorCode.INVALID_INPUT.getMessage(),
                        "REVIEW_IMAGE_DISPLAY_ORDER_DUPLICATED"
                );
            }
        }

        // 1) 트랜잭션 안: Image(TMP) 저장 + 리뷰 매핑 생성
        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        for (ImageAttachmentRequestDto dto : images) {
            String tmpKey = dto.tmpKey();

            if (tmpKey == null || tmpKey.isBlank() || !tmpKey.startsWith("tmp/")) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_TMP_KEY,
                        ErrorCode.INVALID_TMP_KEY.getMessage(),
                        "REVIEW_IMAGE_TMPKEY_INVALID"
                );
            }

            Image img = new Image();
            img.setUser(user);
            img.setS3Key(tmpKey);
            img.setStatus(ImageStatus.TMP);
            imageRepository.save(img);

            ReviewImageMapping mapping = ReviewImageMapping.builder()
                    .image(img)
                    .review(review)
                    .eventId(review.getEvent().getId())
                    .displayOrder(dto.displayOrder() == null ? 0 : dto.displayOrder())
                    .reviewStatus(ReviewStatus.VISIBLE)
                    .build();
            review.addImageMapping(mapping);

            created.add(new NewImage(img.getId(), tmpKey));
        }

        // 2) 트랜잭션 커밋 후: tmp→inline 승격 + URL 반영 + 파생본(리뷰 프리셋) 생성
        for (NewImage ni : created) {
            afterCommitExecutor.run(() -> {
                String inlineKey = imageService.promoteAndCommit(ni.id(), ni.tmpKey());

                s3Waiter.waitUntilExists(inlineKey);

                try {
                    imageVariantService.generateVariants(inlineKey, AssetVariantPreset.REVIEW);
                } catch (Throwable t) {
                    log.error(
                            "[variants] generation failed but ignored. imageId={}, key={}",
                            ni.id(), inlineKey, t
                    );
                }
            });
        }
    }

    /**
     * 리뷰 수정 시 이미지 일괄 갱신
     *  - reqs: 남길/추가할 이미지 전체 목록
     *  - 비어 있으면 모두 삭제
     */
    @Transactional
    public void update(Review review, List<ImageAttachmentUpdateRequestDto> reqs, UUID userId) {
        if (reqs == null) return;
        if (reqs.isEmpty()) {
            deleteAll(review.getId());
            return;
        }

        // displayOrder & 요청 형식 검증
        Set<Integer> orders = new HashSet<>();
        for (var r : reqs) {
            Integer ord = (r.displayOrder() == null) ? 0 : r.displayOrder();
            if (!orders.add(ord)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        ErrorCode.INVALID_INPUT.getMessage(),
                        "REVIEW_IMAGE_DISPLAY_ORDER_DUPLICATED"
                );
            }

            boolean hasId  = r.id() != null;
            boolean hasTmp = r.tmpKey() != null && !r.tmpKey().isBlank();

            if (hasTmp && !r.tmpKey().startsWith("tmp/")) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_TMP_KEY,
                        ErrorCode.INVALID_TMP_KEY.getMessage(),
                        "REVIEW_IMAGE_TMPKEY_INVALID"
                );
            }

            if (hasId == hasTmp) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        ErrorCode.INVALID_INPUT.getMessage(),
                        "REVIEW_IMAGE_ID_OR_TMPKEY_XOR_REQUIRED"
                );
            }
        }

        // 1) 이전 스냅샷
        List<ReviewImageMapping> beforeMappings =
                reviewImageMappingRepository.findByReviewId(review.getId());
        List<Long> beforeIds = beforeMappings.stream()
                .map(m -> m.getImage().getId())
                .toList();

        // 2) 기존 매핑 삭제
        reviewImageMappingRepository.deleteByReviewId(review.getId());

        // 3) 최종 이미지 id/순서 구성
        Map<Long, Integer> orderById = new HashMap<>();
        List<Long> finalIds = new ArrayList<>();

        // 3-1) 기존 유지(id)
        for (var r : reqs) {
            if (r.id() != null) {
                Long imageId = r.id();

                // 존재하는 이미지인지 검증
                if (!imageRepository.existsById(imageId)) {
                    throw BusinessException.withMessageAndDetail(
                            ErrorCode.NOT_FOUND,
                            "존재하지 않는 이미지입니다.",
                            "REVIEW_IMAGE_ID_NOT_FOUND"
                    );
                }

                Integer ord = (r.displayOrder() == null) ? 0 : r.displayOrder();
                finalIds.add(imageId);
                orderById.put(imageId, ord);
            }
        }

        // 3-2) 신규 추가(tmpKey)
        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        for (var r : reqs) {
            if (r.tmpKey() != null && !r.tmpKey().isBlank()) {
                String tmpKey = r.tmpKey();

                if (!tmpKey.startsWith("tmp/")) {
                    throw BusinessException.withMessageAndDetail(
                            ErrorCode.INVALID_TMP_KEY,
                            ErrorCode.INVALID_TMP_KEY.getMessage(),
                            "REVIEW_IMAGE_TMPKEY_INVALID"
                    );
                }

                Image img = new Image();
                img.setUser(review.getUser());
                img.setS3Key(tmpKey);
                img.setStatus(ImageStatus.TMP);
                imageRepository.save(img);

                Integer ord = (r.displayOrder() == null) ? 0 : r.displayOrder();
                finalIds.add(img.getId());
                orderById.put(img.getId(), ord);

                created.add(new NewImage(img.getId(), tmpKey));
            }
        }

        // 4) 매핑 재생성
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

        // 5) after-commit: TMP → inline 승격 + 파생본 생성(리뷰 프리셋)
        for (NewImage ni : created) {
            afterCommitExecutor.run(() -> assetOps.commitExistingAndGenerateVariants(
                    AssetOps.CommitCommand.builder()
                            .imageId(ni.id())
                            .tmpKey(ni.tmpKey())
                            .userId(userId)
                            .preset(AssetVariantPreset.REVIEW)
                            .build()
            ));
        }

        // 6) 고아 처리 (before - final)
        Set<Long> finalSet = new HashSet<>(finalIds);
        List<Long> removed = beforeIds.stream()
                .filter(id -> !finalSet.contains(id))
                .toList();
        handleOrphans(removed);
    }

    @Transactional
    public void deleteAll(Long reviewId) {
        List<Long> before = reviewImageMappingRepository.findByReviewId(reviewId)
                .stream()
                .map(m -> m.getImage().getId())
                .toList();

        reviewImageMappingRepository.deleteByReviewId(reviewId);
        handleOrphans(before);
    }

    @Transactional
    public void delete(Long reviewId) {
        List<Long> before = reviewImageMappingRepository.findByReviewId(reviewId)
                .stream()
                .map(m -> m.getImage().getId())
                .toList();

        reviewImageMappingRepository.deleteByReviewId(reviewId);
        handleOrphans(before);
    }

    private void handleOrphans(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return;
        for (Long id : imageIds) {
            if (reviewImageMappingRepository.countUsages(id) == 0) {
                imageService.softDeleteIfNotYet(id);
            }
        }
    }
}