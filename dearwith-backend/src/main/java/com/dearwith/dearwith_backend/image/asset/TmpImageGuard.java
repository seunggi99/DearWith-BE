package com.dearwith.dearwith_backend.image.asset;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.AwsS3ClientAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TmpImageGuard {

    private final AwsS3ClientAdapter s3Adapter;

    public void ensureExists(String tmpKey) {
        if (tmpKey == null || tmpKey.isBlank()) {
            return;
        }

        if (!s3Adapter.exists(tmpKey)) {
            throw BusinessException.withAll(
                    ErrorCode.IMAGE_PROCESSING_FAILED,
                    ErrorCode.IMAGE_PROCESSING_FAILED.getMessage(),
                    "TMP_KEY_NOT_FOUND:" + tmpKey,
                    "S3에 tmp 이미지가 존재하지 않습니다. 이미 다른 요청에서 사용됐을 가능성이 큽니다.",
                    null
            );
        }
    }

    public void ensureAllExists(Iterable<String> tmpKeys) {
        if (tmpKeys == null) return;
        for (String key : tmpKeys) {
            ensureExists(key);
        }
    }
}