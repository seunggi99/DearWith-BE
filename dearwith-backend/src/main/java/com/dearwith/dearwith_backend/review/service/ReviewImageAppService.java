package com.dearwith.dearwith_backend.review.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.image.asset.AssetOps;
import com.dearwith.dearwith_backend.image.asset.AssetVariantPreset;
import com.dearwith.dearwith_backend.image.asset.TmpImageGuard;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import com.dearwith.dearwith_backend.image.dto.ImageAttachmentUpdateRequestDto;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.enums.ImageStatus;
import com.dearwith.dearwith_backend.image.repository.ImageRepository;
import com.dearwith.dearwith_backend.image.service.AbstractImageSupport;
import com.dearwith.dearwith_backend.image.service.ImageService;
import com.dearwith.dearwith_backend.review.entity.Review;
import com.dearwith.dearwith_backend.review.entity.ReviewImageMapping;
import com.dearwith.dearwith_backend.review.enums.ReviewStatus;
import com.dearwith.dearwith_backend.review.repository.ReviewImageMappingRepository;
import com.dearwith.dearwith_backend.external.aws.AfterCommitExecutor;
import com.dearwith.dearwith_backend.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class ReviewImageAppService extends AbstractImageSupport {

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

    /**
     * 리뷰 생성 시 이미지 등록
     */
    @Transactional
    public void create(Review review, List<ImageAttachmentRequestDto> images, User user) {
        if (images == null || images.isEmpty()) {
            return;
        }

        // tmpKey S3 존재 검증 (중복 제거, null/blank 제외)
        validateTmpKeys(
                images.stream()
                        .map(ImageAttachmentRequestDto::tmpKey)
                        .toList()
        );

        // displayOrder 중복 검증
        Set<Integer> seen = new HashSet<>();
        for (ImageAttachmentRequestDto dto : images) {
            Integer ord = safeOrder(dto.displayOrder());
            if (!seen.add(ord)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        "이미지 등록 중 오류가 발생했습니다.",
                        "REVIEW_IMAGE_DISPLAY_ORDER_DUPLICATED"
                );
            }
        }

        // 1) 트랜잭션 안: Image(TMP) 저장 + 리뷰 매핑 생성
        record NewImage(Long id, String tmpKey) { }
        List<NewImage> created = new ArrayList<>();

        for (ImageAttachmentRequestDto dto : images) {
            String tmpKey = dto.tmpKey();

            if (!hasTmp(tmpKey) || !tmpKey.startsWith("tmp/")) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_TMP_KEY,
                        "이미지 등록 중 오류가 발생했습니다.",
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
                    .displayOrder(safeOrder(dto.displayOrder()))
                    .reviewStatus(ReviewStatus.VISIBLE)
                    .build();
            review.addImageMapping(mapping);

            created.add(new NewImage(img.getId(), tmpKey));
        }

        // 2) 트랜잭션 커밋 후: AssetOps로 TMP → inline + REVIEW 프리셋 파생본 생성
        for (NewImage ni : created) {
            afterCommitExecutor.run(() -> {
                try {
                    assetOps.commitExistingAndGenerateVariants(
                            AssetOps.CommitCommand.builder()
                                    .imageId(ni.id())
                                    .tmpKey(ni.tmpKey())
                                    .userId(user.getId())
                                    .preset(AssetVariantPreset.REVIEW)
                                    .build()
                    );
                } catch (Throwable t) {
                    log.error(
                            "[review-image] commitExistingAndGenerateVariants (create) failed. imageId={}, tmpKey={}",
                            ni.id(), ni.tmpKey(), t
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

        // tmpKey S3 존재 검증
        validateTmpKeys(
                reqs.stream()
                        .map(ImageAttachmentUpdateRequestDto::tmpKey)
                        .toList()
        );

        // displayOrder & 요청 형식 검증
        Set<Integer> orders = new HashSet<>();
        for (var r : reqs) {
            Integer ord = safeOrder(r.displayOrder());
            if (!orders.add(ord)) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        "이미지 등록 중 오류가 발생했습니다.",
                        "REVIEW_IMAGE_DISPLAY_ORDER_DUPLICATED"
                );
            }

            boolean hasId  = r.id() != null;
            boolean hasTmp = hasTmp(r.tmpKey());

            if (hasTmp && !r.tmpKey().startsWith("tmp/")) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_TMP_KEY,
                        "이미지 등록 중 오류가 발생했습니다.",
                        "REVIEW_IMAGE_TMPKEY_INVALID"
                );
            }

            if (hasId == hasTmp) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        "이미지 등록 중 오류가 발생했습니다.",
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

                if (!imageRepository.existsById(imageId)) {
                    throw BusinessException.withMessageAndDetail(
                            ErrorCode.NOT_FOUND,
                            "이미지 등록 중 오류가 발생했습니다.",
                            "REVIEW_IMAGE_ID_NOT_FOUND"
                    );
                }

                Integer ord = safeOrder(r.displayOrder());
                finalIds.add(imageId);
                orderById.put(imageId, ord);
            }
        }

        // 3-2) 신규 추가(tmpKey)
        record NewImage(Long id, String tmpKey) { }
        List<NewImage> created = new ArrayList<>();

        for (var r : reqs) {
            if (hasTmp(r.tmpKey())) {
                String tmpKey = r.tmpKey();

                if (!tmpKey.startsWith("tmp/")) {
                    throw BusinessException.withMessageAndDetail(
                            ErrorCode.INVALID_TMP_KEY,
                            "이미지 등록 중 오류가 발생했습니다.",
                            "REVIEW_IMAGE_TMPKEY_INVALID"
                    );
                }

                Image img = new Image();
                img.setUser(review.getUser());
                img.setS3Key(tmpKey);
                img.setStatus(ImageStatus.TMP);
                imageRepository.save(img);

                Integer ord = safeOrder(r.displayOrder());
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
            afterCommitExecutor.run(() -> {
                try {
                    assetOps.commitExistingAndGenerateVariants(
                            AssetOps.CommitCommand.builder()
                                    .imageId(ni.id())
                                    .tmpKey(ni.tmpKey())
                                    .userId(userId)
                                    .preset(AssetVariantPreset.REVIEW)
                                    .build()
                    );
                } catch (Throwable t) {
                    log.error(
                            "[review-image] commitExistingAndGenerateVariants (update) failed. imageId={}, tmpKey={}",
                            ni.id(), ni.tmpKey(), t
                    );
                }
            });
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