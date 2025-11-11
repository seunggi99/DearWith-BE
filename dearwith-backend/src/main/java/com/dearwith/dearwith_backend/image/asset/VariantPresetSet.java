package com.dearwith.dearwith_backend.image.asset;


import lombok.AccessLevel;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder(access = AccessLevel.PUBLIC)
public class VariantPresetSet {
    @Singular
    List<VariantSpec> specs;
}