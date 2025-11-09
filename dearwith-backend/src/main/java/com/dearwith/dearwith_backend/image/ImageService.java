package com.dearwith.dearwith_backend.image;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final ImageVariantService imageVariantService;
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
    public Image registerCommittedImageWithVariants(String originalKey, UUID userId, List<VariantSpec> specs) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String url = assetUrlService.generatePublicUrl(originalKey);

        Image image = Image.builder()
                .s3Key(originalKey)
                .imageUrl(url)
                .status(ImageStatus.COMMITTED)
                .user(user)
                .build();

        Image saved = imageRepository.save(image);

        imageVariantService.generateVariants(originalKey, specs);

        return saved;
    }
}
