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
import com.dearwith.dearwith_backend.image.service.*;
import com.dearwith.dearwith_backend.review.entity.Review;
import com.dearwith.dearwith_backend.review.entity.ReviewImageMapping;
import com.dearwith.dearwith_backend.review.enums.ReviewStatus;
import com.dearwith.dearwith_backend.review.repository.ReviewImageMappingRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    @Transactional
    public void create(Review review, List<ImageAttachmentRequestDto> images, User user) {

        if (images == null || images.isEmpty()) {
            return;
        }

        Set<Integer> seen = new HashSet<>();
        for (ImageAttachmentRequestDto dto : images) {
            Integer ord = dto.displayOrder() == null ? 0 : dto.displayOrder();
            if (!seen.add(ord)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "displayOrder가 중복되었습니다.");
            }
        }

        // 1) 트랜잭션 안: Image(TMP) 저장 + 리뷰 매핑 생성
        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        for (ImageAttachmentRequestDto dto : images) {
            String tmpKey = dto.tmpKey();

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

                imageVariantService.generateVariants(inlineKey, AssetVariantPreset.REVIEW);
            });
        }
    }

    @Transactional
    public void update(Review review, List<ImageAttachmentUpdateRequestDto> reqs, UUID userId){
        if (reqs == null) return;
        if (reqs.isEmpty()) {
            deleteAll(review.getId());
            return;
        }

        Set<Integer> orders = new HashSet<>();
        for (var r : reqs) {
            if (!orders.add(r.displayOrder()))
                throw new BusinessException(ErrorCode.INVALID_INPUT, "displayOrder가 중복되었습니다.");

            boolean hasId  = r.id() != null;
            boolean hasTmp = r.tmpKey() != null && !r.tmpKey().isBlank();
            if (hasId == hasTmp)
                throw new BusinessException(ErrorCode.INVALID_INPUT, "각 항목은 id 또는 tmpKey 중 하나만 제공해야 합니다.");
        }

        // 1) 이전 스냅샷
        List<ReviewImageMapping> beforeMappings = reviewImageMappingRepository.findByReviewId(review.getId());
        List<Long> beforeIds = beforeMappings.stream().map(m -> m.getImage().getId()).toList();

        // 2) 기존 매핑 삭제
        reviewImageMappingRepository.deleteByReviewId(review.getId());

        // 3) 최종 이미지 id/순서 구성
        Map<Long, Integer> orderById = new HashMap<>();
        List<Long> finalIds = new ArrayList<>();

        // 3-1) 기존 유지(id)
        for (var r : reqs) {
            if (r.id() != null) {
                finalIds.add(r.id());
                orderById.put(r.id(), r.displayOrder());
            }
        }

        // 3-2) 신규 추가
        record NewImage(Long id, String tmpKey) {}
        List<NewImage> created = new ArrayList<>();

        for (var r : reqs) {
            if (r.tmpKey() != null && !r.tmpKey().isBlank()) {
                Image img = new Image();
                img.setUser(review.getUser());
                img.setS3Key(r.tmpKey());
                img.setStatus(ImageStatus.TMP);
                imageRepository.save(img);

                finalIds.add(img.getId());
                orderById.put(img.getId(), r.displayOrder());

                created.add(new NewImage(img.getId(), r.tmpKey()));
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

        // 5) after-commit: TMP → inline 승격 + URL 세팅 + S3 존재 대기 + 변환 생성(리뷰 프리셋)
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
        List<Long> removed = beforeIds.stream().filter(id -> !finalSet.contains(id)).toList();
        handleOrphans(removed);
    }
    @Transactional
    public void deleteAll(Long reviewId) {
        List<Long> before = reviewImageMappingRepository.findByReviewId(reviewId)
                .stream().map(m -> m.getImage().getId()).toList();

        reviewImageMappingRepository.deleteByReviewId(reviewId);
        handleOrphans(before);
    }

    @Transactional
    public void delete(Long reviewId) {
        List<Long> before = reviewImageMappingRepository.findByReviewId(reviewId)
                .stream().map(m -> m.getImage().getId()).toList();

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
