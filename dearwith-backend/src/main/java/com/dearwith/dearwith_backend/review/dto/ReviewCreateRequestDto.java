package com.dearwith.dearwith_backend.review.dto;


import com.dearwith.dearwith_backend.image.ImageAttachmentRequest;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.NotBlank;

import java.util.List;

public record ReviewCreateRequestDto(
        @NotBlank @Size(max = 300)
        String content,

        @Size(max = 4)
        List<String> tags,

        @Size(max = 2)
        List<ImageAttachmentRequest> images
) {}