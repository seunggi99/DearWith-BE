package com.dearwith.dearwith_backend.auth.service;

import com.dearwith.dearwith_backend.auth.dto.KakaoTokenResponseDto;
import com.dearwith.dearwith_backend.auth.dto.KakaoUserInfoDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";


    public String getAccessToken(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", authorizationCode);
        body.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<KakaoTokenResponseDto> response = restTemplate.postForEntity(
                TOKEN_URI, request, KakaoTokenResponseDto.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        return response.getBody().getAccess_token();
    }

    public KakaoUserInfoDto getUserInfo(String accessToken) {
        // Authorization 헤더 구성
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        // 사용자 정보 요청
        ResponseEntity<Map> response = restTemplate.exchange(
                USER_INFO_URI, HttpMethod.GET, request, Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        Map<String, Object> body = response.getBody();

        // 기본 ID 추출
        Long id = Long.valueOf(body.get("id").toString());
        Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");

        // 이메일, 닉네임 추출 (비즈앱이 아닐 경우 null일 수 있음)
        String email = null;
        String nickname = "unknown";

        if (kakaoAccount != null) {
            Object emailObj = kakaoAccount.get("email");
            if (emailObj != null) email = emailObj.toString();

            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null && profile.get("nickname") != null) {
                nickname = profile.get("nickname").toString();
            }
        }

        return KakaoUserInfoDto.builder()
                .id(id)
                .email(email) // 비즈앱이 아니면 null로 저장
                .nickname(nickname)
                .build();
    }
}
