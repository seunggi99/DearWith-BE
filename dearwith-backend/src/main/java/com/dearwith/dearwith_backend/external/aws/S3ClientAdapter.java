package com.dearwith.dearwith_backend.external.aws;

import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.Duration;
import java.util.List;

public interface S3ClientAdapter {

    /** 단건 객체 존재 여부 */
    boolean exists(String key);

    /** 단건 객체 메타 조회 (없으면 예외 던질 수 있음) */
    HeadObjectResponse head(String key);

    /** 단건 업로드 */
    void put(String key, byte[] bytes, String contentType, String cacheControl);

    /** 단건 복사 */
    void copy(String srcKey, String dstKey, boolean replaceMetadata, String contentType, String cacheControl);

    /** 단건 삭제 */
    void delete(String key);

    /** prefix 아래의 모든 객체 key 나열 (필요 시 내부에서 페이징 반복) */
    List<String> listAllKeys(String prefix);

    /** Presigned PUT 생성 */
    record PresignResult(String url, String key, long ttlSeconds) {}
    PresignResult presignPut(String key, String contentType, Duration ttl);

    byte[] read(String key);
}
