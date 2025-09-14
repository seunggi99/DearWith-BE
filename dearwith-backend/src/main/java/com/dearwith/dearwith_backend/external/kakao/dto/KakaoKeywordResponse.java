package com.dearwith.dearwith_backend.external.kakao.dto;

import java.util.List;

public record KakaoKeywordResponse(
        KakaoMeta meta,
        List<KakaoPlace> documents
) {}