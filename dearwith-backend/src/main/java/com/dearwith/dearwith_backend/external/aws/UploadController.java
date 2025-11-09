package com.dearwith.dearwith_backend.external.aws;


import com.dearwith.dearwith_backend.image.Image;
import com.dearwith.dearwith_backend.image.ImageAssetService;
import com.dearwith.dearwith_backend.image.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final ImageService imageService;

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

    @PostMapping("/commit")
    @Operation(
            summary = "tmp → inline 승격(Commit) + DB 저장",
            description = """
                    프론트가 presign URL로 S3에 업로드 완료 후 호출합니다.
                    서버는 tmp 키를 inline으로 승격(copy)하고, DB에 Image 레코드를 저장합니다.
                    응답의 url은 프론트에서 바로 <img src>로 사용 가능합니다.
                    예시 요청: {"tmpKey":"tmp/event/.../example.png"}
                    """)
    public CommitRes commit(@Valid @RequestBody CommitReq req,
                            @AuthenticationPrincipal(expression = "id") UUID userId) {
        String inlineKey = imageAssetService.promoteTmpToInline(req.tmpKey());
        Image saved = imageService.registerCommittedImage(inlineKey, userId);
        return new CommitRes(saved.getId(), saved.getImageUrl());
    }

    public record PresignReq(String filename, String contentType, String domain) {}
    public record PresignRes(String url, String key, long ttl) {}
    public record CommitReq(String tmpKey) {}
    public record CommitRes(Long imageId, String url) {}
}
