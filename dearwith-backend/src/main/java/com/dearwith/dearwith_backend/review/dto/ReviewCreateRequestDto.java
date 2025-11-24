package com.dearwith.dearwith_backend.review.dto;


import com.dearwith.dearwith_backend.image.dto.ImageAttachmentRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ReviewCreateRequestDto(
        @NotBlank(message = "리뷰 내용은 필수입니다.")
        @Size(max = 300, message = "리뷰는 300자 이하만 가능합니다.")
        String content,
        @Size(max = 2,message = "이미지는 최대 2개까지만 등록할 수 있습니다.")
        List<@Valid ImageAttachmentRequestDto> images,
        @Size(max = 4,message = "태그는 최대 4개까지 가능합니다.")
        List<@NotBlank String> tags
) {}