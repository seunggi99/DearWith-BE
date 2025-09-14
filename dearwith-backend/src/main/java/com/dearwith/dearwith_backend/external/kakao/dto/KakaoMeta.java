package com.dearwith.dearwith_backend.external.kakao.dto;

public record KakaoMeta(
        int total_count,
        int pageable_count,
        boolean is_end
) {}
