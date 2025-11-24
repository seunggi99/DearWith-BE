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


    /*──────────────────────────────────────────────
     | 0. key 검증 공통 메서드
     *──────────────────────────────────────────────*/
    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    ErrorCode.INVALID_INPUT.getMessage(),
                    "S3_KEY_EMPTY_OR_NULL"
            );
        }
    }


    /*──────────────────────────────────────────────
     | 1. exists
     *──────────────────────────────────────────────*/
    @Override
    public boolean exists(String key) {
        try {
            validateKey(key);
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }


    /*──────────────────────────────────────────────
     | 2. head
     *──────────────────────────────────────────────*/
    @Override
    public HeadObjectResponse head(String key) {
        validateKey(key);
        try {
            return s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (NoSuchKeyException e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OBJECT_NOT_FOUND,
                    ErrorCode.S3_OBJECT_NOT_FOUND.getMessage(),
                    "S3_HEAD_NO_SUCH_KEY: " + key
            );
        } catch (S3Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "S3_HEAD_ERROR: " + e.getMessage()
            );
        }
    }


    /*──────────────────────────────────────────────
     | 3. put
     *──────────────────────────────────────────────*/
    @Override
    public void put(String key, byte[] bytes, String contentType, String cacheControl) {
        validateKey(key);

        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .cacheControl(cacheControl)
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
        } catch (S3Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "S3_PUT_FAILED key=" + key + " err=" + e.getMessage()
            );
        }
    }


    /*──────────────────────────────────────────────
     | 4. copy
     *──────────────────────────────────────────────*/
    @Override
    public void copy(String srcKey, String dstKey, boolean replaceMetadata, String contentType, String cacheControl) {

        validateKey(srcKey);
        validateKey(dstKey);

        try {
            CopyObjectRequest.Builder b = CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(srcKey)
                    .destinationBucket(bucket)
                    .destinationKey(dstKey);

            if (replaceMetadata) {
                b.metadataDirective(MetadataDirective.REPLACE)
                        .contentType(contentType)
                        .cacheControl(cacheControl);
            } else {
                b.metadataDirective(MetadataDirective.COPY);
            }

            s3.copyObject(b.build());

        } catch (NoSuchKeyException e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OBJECT_NOT_FOUND,
                    ErrorCode.S3_OBJECT_NOT_FOUND.getMessage(),
                    "S3_COPY_SOURCE_NOT_FOUND src=" + srcKey
            );
        } catch (S3Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "S3_COPY_FAILED src=" + srcKey + " dst=" + dstKey + " err=" + e.getMessage()
            );
        }
    }


    /*──────────────────────────────────────────────
     | 5. delete
     *──────────────────────────────────────────────*/
    @Override
    public void delete(String key) {
        validateKey(key);
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "S3_DELETE_FAILED key=" + key + " err=" + e.getMessage()
            );
        }
    }


    /*──────────────────────────────────────────────
     | 6. listAllKeys
     *──────────────────────────────────────────────*/
    @Override
    public List<String> listAllKeys(String prefix) {
        validateKey(prefix);

        List<String> keys = new ArrayList<>();
        String token = null;
        try {
            do {
                ListObjectsV2Response resp = s3.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .continuationToken(token)
                        .build());

                resp.contents().forEach(o -> keys.add(o.key()));
                token = resp.isTruncated() ? resp.nextContinuationToken() : null;

            } while (token != null);

            return keys;

        } catch (S3Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "S3_LIST_FAILED prefix=" + prefix + " err=" + e.getMessage()
            );
        }
    }


    /*──────────────────────────────────────────────
     | 7. read
     *──────────────────────────────────────────────*/
    @Override
    public byte[] read(String key) {
        validateKey(key);

        try (var input = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
             var baos = new ByteArrayOutputStream()) {

            input.transferTo(baos);
            return baos.toByteArray();

        } catch (NoSuchKeyException e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OBJECT_NOT_FOUND,
                    ErrorCode.S3_OBJECT_NOT_FOUND.getMessage(),
                    "S3_READ_NO_SUCH_KEY: " + key
            );
        } catch (Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "S3_READ_FAILED key=" + key + " err=" + e.getMessage()
            );
        }
    }


    /*──────────────────────────────────────────────
     | 8. presignPut
     *──────────────────────────────────────────────*/
    @Override
    public PresignResult presignPut(String key, String contentType, Duration ttl) {
        validateKey(key);

        try {
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

        } catch (Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.S3_OPERATION_FAILED,
                    ErrorCode.S3_OPERATION_FAILED.getMessage(),
                    "S3_PRESIGN_PUT_FAILED key=" + key + " err=" + e.getMessage()
            );
        }
    }
}