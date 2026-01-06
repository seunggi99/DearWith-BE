package com.dearwith.dearwith_backend.external.aws;


import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.image.service.ImageAssetService;
import com.dearwith.dearwith_backend.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final ImageAssetService imageAssetService;

    @PostMapping("/presign")
    @Operation(
            summary = "이미지 업로드용 S3 Presigned URL 발급",
            description = """
        업로드 전 S3에 직접 PUT할 수 있는 Presigned URL을 발급합니다.
        도메인별로 tmp/{domain}/{filename} 경로에 저장됩니다.
        도메인 : "event", "review", "artist", "profile"
        예시 요청:
        {
          "filename": "example.png",
          "contentType": "image/png",
          "domain": "event"
        }

        예시 응답:
        {
          "url": "https://s3-.../tmp/event/example.png?...",
          "key": "tmp/event/example.png",
          "ttl": 300
        }
        """
    )
    public PresignRes presign(@RequestBody PresignReq req) {
        var out = imageAssetService.presignTmpPut(
                req.domain(), req.filename(), req.contentType(), Duration.ofMinutes(5));

        return new PresignRes(out.url(), out.key(), out.ttlSeconds());
    }

    public record PresignReq(String filename, String contentType, String domain) {}
    public record PresignRes(String url, String key, long ttl) {}
}
