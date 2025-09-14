package com.dearwith.dearwith_backend.external.kakao;

import com.dearwith.dearwith_backend.external.kakao.dto.KakaoKeywordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KakaoLocalService {
    private final WebClient kakaoWebClient;
    public KakaoKeywordResponse searchByKeyword(
            String query, Double x, Double y, Integer radius, Integer page, Integer size
    ) {
        return kakaoWebClient.get()
                .uri(uri -> uri.path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .queryParamIfPresent("x", Optional.ofNullable(x))
                        .queryParamIfPresent("y", Optional.ofNullable(y))
                        .queryParamIfPresent("radius", Optional.ofNullable(radius))
                        .queryParamIfPresent("page", Optional.ofNullable(page))
                        .queryParamIfPresent("size", Optional.ofNullable(size))
                        .build())
                .retrieve()
                .bodyToMono(KakaoKeywordResponse.class)
                .block();
    }

    /**
     * 주소 문자열로 검색(지번/도로명 포함) — 필요 시 사용
     * 키워드 검색과 응답 구조가 동일하여 동일 DTO로 수신
     */
    public KakaoKeywordResponse searchByAddress(String query, Integer page, Integer size) {
        return kakaoWebClient.get()
                .uri(uri -> uri.path("/v2/local/search/address.json")
                        .queryParam("query", query)
                        .queryParamIfPresent("page", Optional.ofNullable(page))
                        .queryParamIfPresent("size", Optional.ofNullable(size))
                        .build())
                .retrieve()
                .bodyToMono(KakaoKeywordResponse.class)
                .block();
    }
}
