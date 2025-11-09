package com.dearwith.dearwith_backend.external.aws;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.io.ByteArrayOutputStream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3ClientAdapter implements S3ClientAdapter {

    private final S3Client s3;
    private final S3Presigner s3Presigner;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Override
    public boolean exists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    @Override
    public HeadObjectResponse head(String key) {
        try {
            return s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "S3 객체를 찾을 수 없습니다. key=" + key);
        }
    }

    @Override
    public void put(String key, byte[] bytes, String contentType, String cacheControl) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .cacheControl(cacheControl)
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.S3_OPERATION_FAILED, "S3 업로드 실패 key=" + key);
        }
    }

    @Override
    public void copy(String srcKey, String dstKey, boolean replaceMetadata, String contentType, String cacheControl) {
        try {
            CopyObjectRequest.Builder b = CopyObjectRequest.builder()
                    .sourceBucket(bucket).sourceKey(srcKey)
                    .destinationBucket(bucket).destinationKey(dstKey);

            if (replaceMetadata) {
                b.metadataDirective(MetadataDirective.REPLACE)
                        .contentType(contentType)
                        .cacheControl(cacheControl);
            } else {
                b.metadataDirective(MetadataDirective.COPY);
            }
            s3.copyObject(b.build());
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.S3_OPERATION_FAILED, "S3 복사 실패 src=" + srcKey + " dst=" + dstKey);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.S3_OPERATION_FAILED, "S3 삭제 실패 key=" + key);
        }
    }

    @Override
    public List<String> listAllKeys(String prefix) {
        List<String> keys = new ArrayList<>();
        String token = null;
        do {
            ListObjectsV2Response resp = s3.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucket).prefix(prefix).continuationToken(token).build());
            for (S3Object o : resp.contents()) keys.add(o.key());
            token = resp.isTruncated() ? resp.nextContinuationToken() : null;
        } while (token != null);
        return keys;
    }

    @Override
    public byte[] read(String key) {
        try (var in = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
             var baos = new ByteArrayOutputStream()) {
            in.transferTo(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.S3_OPERATION_FAILED, "S3 다운로드 실패 key=" + key);
        }
    }

    @Override
    public PresignResult presignPut(String key, String contentType, Duration ttl) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        var presigned = s3Presigner.presignPutObject(b -> b
                .signatureDuration(ttl)
                .putObjectRequest(objectRequest)
        );
        return new PresignResult(presigned.url().toString(), key, ttl.toSeconds());
    }
}
