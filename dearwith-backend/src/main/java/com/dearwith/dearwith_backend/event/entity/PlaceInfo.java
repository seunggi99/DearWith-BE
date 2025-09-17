package com.dearwith.dearwith_backend.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceInfo {
    @Column(name = "place_kakao_id", length = 64)
    private String kakaoPlaceId;    // Kakao id
    @Column(name = "place_name", length = 200)
    private String name;            // place_name
    @Column(name = "place_addr_road", length = 300)
    private String roadAddress;        // road_address_name
    @Column(name = "place_addr_jibun", length = 300)
    private String jibunAddress;       // address_name
    @Column(name = "place_lon", precision = 10, scale = 7)
    private BigDecimal lon;         // x
    @Column(name = "place_lat", precision = 10, scale = 7)
    private BigDecimal lat;         // y
    @Column(name = "place_phone", length = 50)
    private String phone;
    @Column(name = "place_url", length = 500)
    private String placeUrl;
}