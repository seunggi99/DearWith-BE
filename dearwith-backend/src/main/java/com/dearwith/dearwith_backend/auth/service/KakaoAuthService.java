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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
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


    /*──────────────────────────────────────────────
     | 1) 카카오 Access Token 가져오기
     *──────────────────────────────────────────────*/
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

        try {
            ResponseEntity<KakaoTokenResponseDto> response = restTemplate.postForEntity(
                    TOKEN_URI, request, KakaoTokenResponseDto.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw BusinessException.withAll(
                        ErrorCode.KAKAO_AUTH_FAILED,
                        null,
                        "KAKAO_TOKEN_NULL",
                        "Kakao token API returned non-2xx or null body",
                        null
                );
            }

            String accessToken = response.getBody().getAccess_token();
            if (accessToken == null || accessToken.isBlank()) {
                throw BusinessException.withMessage(
                        ErrorCode.KAKAO_AUTH_FAILED,
                        "카카오 인증 정보를 가져올 수 없습니다."
                );
            }

            return accessToken;

        } catch (HttpClientErrorException e) {
            throw BusinessException.withAll(
                    ErrorCode.KAKAO_AUTH_FAILED,
                    null,
                    "KAKAO_TOKEN_HTTP_ERROR",
                    "Kakao token API error: status=" + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(),
                    e
            );

        } catch (RestClientException e) {
            throw BusinessException.withAll(
                    ErrorCode.KAKAO_AUTH_FAILED,
                    null,
                    "KAKAO_TOKEN_RESTCLIENT_ERROR",
                    "Kakao token request failed: " + e.getMessage(),
                    e
            );

        } catch (Exception e) {
            throw BusinessException.withAll(
                    ErrorCode.KAKAO_AUTH_FAILED,
                    null,
                    "KAKAO_TOKEN_UNKNOWN_ERROR",
                    "Unexpected token API error: " + e.getMessage(),
                    e
            );
        }
    }



    /*──────────────────────────────────────────────
     | 2) 카카오 사용자 정보 가져오기
     *──────────────────────────────────────────────*/
    public KakaoUserInfoDto getUserInfo(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    USER_INFO_URI, HttpMethod.GET, request, Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw BusinessException.withAll(
                        ErrorCode.KAKAO_AUTH_FAILED,
                        null,
                        "KAKAO_USERINFO_NULL",
                        "Kakao user info API returned non-2xx or null body",
                        null
                );
            }

            Map<String, Object> body = response.getBody();

            // ID
            Object idObj = body.get("id");
            if (idObj == null) {
                throw BusinessException.withAll(
                        ErrorCode.KAKAO_AUTH_FAILED,
                        null,
                        "KAKAO_ID_NULL",
                        "Kakao user info missing id field",
                        null
                );
            }

            Long id = Long.valueOf(idObj.toString());

            // kakao_account
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");

            String email = null;
            String nickname = "사용자";

            if (kakaoAccount != null) {
                Object emailObj = kakaoAccount.get("email");
                if (emailObj != null) {
                    email = emailObj.toString();
                }

                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null && profile.get("nickname") != null) {
                    nickname = profile.get("nickname").toString();
                }
            }

            return KakaoUserInfoDto.builder()
                    .id(id)
                    .email(email)
                    .nickname(nickname)
                    .build();

        } catch (HttpClientErrorException e) {
            throw BusinessException.withAll(
                    ErrorCode.KAKAO_AUTH_FAILED,
                    null,
                    "KAKAO_USERINFO_HTTP_ERROR",
                    "Kakao user info API error: status=" + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(),
                    e
            );

        } catch (RestClientException e) {
            throw BusinessException.withAll(
                    ErrorCode.KAKAO_AUTH_FAILED,
                    null,
                    "KAKAO_USERINFO_RESTCLIENT_ERROR",
                    "Kakao user info request failed: " + e.getMessage(),
                    e
            );

        } catch (Exception e) {
            throw BusinessException.withAll(
                    ErrorCode.KAKAO_AUTH_FAILED,
                    null,
                    "KAKAO_USERINFO_UNKNOWN_ERROR",
                    "Unexpected user info API error: " + e.getMessage(),
                    e
            );
        }
    }
}