package com.dearwith.dearwith_backend.external.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Presigner s3Presigner;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.region}")
    private String region;

    public PresignedPutObjectRequest createPresignedPut(String key, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        return s3Presigner.presignPutObject(
                b -> b.signatureDuration(Duration.ofMinutes(5))
                        .putObjectRequest(objectRequest)
        );
    }

    public String promoteTmpToInline(String tmpKey) {
        String finalKey = tmpKey.replace("tmp/", "inline/");
        try (S3Client s3 = S3Client.builder().region(Region.of(region)).build()) {
            s3.copyObject(b -> b.sourceBucket(bucket).sourceKey(tmpKey)
                    .destinationBucket(bucket).destinationKey(finalKey));
            s3.deleteObject(b -> b.bucket(bucket).key(tmpKey));
        }
        return finalKey;
    }

    public String generatePublicUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
