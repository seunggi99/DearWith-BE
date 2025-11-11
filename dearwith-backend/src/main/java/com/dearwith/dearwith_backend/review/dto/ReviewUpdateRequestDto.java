package com.dearwith.dearwith_backend.review.dto;

import com.dearwith.dearwith_backend.image.dto.ImageAttachmentUpdateRequestDto;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReviewUpdateRequestDto(
        @Nullable
        @Size(max = 300)
        String content,
        @Nullable
        @Size(max = 4)
        List<@NotBlank String> tags,
        @Nullable
        @Size(max = 2)
        List<@Valid ImageAttachmentUpdateRequestDto> images
) {}
