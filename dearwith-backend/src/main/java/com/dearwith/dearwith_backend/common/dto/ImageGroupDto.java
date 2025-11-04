package com.dearwith.dearwith_backend.common.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ImageGroupDto {
    private String group;
    private List<ImageVariantDto> variants;
}
