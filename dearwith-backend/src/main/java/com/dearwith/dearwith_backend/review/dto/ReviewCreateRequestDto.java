package com.dearwith.dearwith_backend.review.dto;


import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ReviewCreateRequestDto(
        @NotBlank @Size(max = 300) String content,
        @Size(max = 2) List<@Valid ImageAttachmentRequestDto> images,
        @Size(max = 4) List<@NotBlank String> tags
) {}