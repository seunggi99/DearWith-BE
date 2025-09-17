package com.dearwith.dearwith_backend.image;

import com.dearwith.dearwith_backend.external.aws.S3UploadService;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final S3UploadService s3UploadService;
    private final UserRepository userRepository;

    public Image commitImage(String finalKey, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String url = s3UploadService.generatePublicUrl(finalKey);

        Image image = Image.builder()
                .s3Key(finalKey)
                .ImageUrl(url)
                .status(ImageStatus.COMMITTED)
                .user(user)
                .build();

        return imageRepository.save(image);
    }
}
