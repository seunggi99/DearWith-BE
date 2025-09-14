package com.dearwith.dearwith_backend.external.kakao.dto;

public record KakaoPlace(
        String id,
        String place_name,
        String address_name,       // 지번
        String road_address_name,  // 도로명
        String x,                  // 경도
        String y,                  // 위도
        String phone,
        String place_url
) {}