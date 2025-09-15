package com.dearwith.dearwith_backend.external.aws;


import com.dearwith.dearwith_backend.image.Image;
import com.dearwith.dearwith_backend.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final S3UploadService s3UploadService;
    private final ImageService imageService;

    @PostMapping("/presign")
    public PresignRes presign(@RequestBody PresignReq req,
                              @AuthenticationPrincipal(expression = "id") UUID ownerId) {
        var presigned = s3UploadService.createPresignedPut("tmp/" + req.filename(), req.contentType());
        return new PresignRes(presigned.url().toString(), "tmp/" + req.filename(), 300);
    }

    @PostMapping("/commit")
    public CommitRes commit(@RequestBody CommitReq req,
                            @AuthenticationPrincipal(expression = "id") UUID userId) {
        Image saved = imageService.commitImage(req.tmpKey(), userId);
        return new CommitRes(saved.getId(), saved.getUrl());
    }

    public record PresignReq(String filename, String contentType) {}
    public record PresignRes(String url, String key, long ttl) {}
    public record CommitReq(String tmpKey) {}
    public record CommitRes(Long imageId, String url) {}
}
