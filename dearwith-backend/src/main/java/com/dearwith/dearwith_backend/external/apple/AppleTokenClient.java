package com.dearwith.dearwith_backend.external.apple;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleTokenClient {

    private final AppleAuthProperties props;
    private final AppleClientSecretGenerator clientSecretGenerator;

    private final WebClient webClient = WebClient.builder().build();

    public AppleTokenResponse exchangeCodeForToken(String authorizationCode) {
        String clientSecret = clientSecretGenerator.generate();

        return webClient.post()
                .uri(props.getSignin().getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("code", authorizationCode)
                        .with("client_id", props.getSignin().getClientId())
                        .with("client_secret", clientSecret))
                .retrieve()
                .bodyToMono(AppleTokenResponse.class)
                .onErrorResume(e -> {
                    log.error("Apple token exchange failed", e);
                    return Mono.error(BusinessException.withMessage(
                            ErrorCode.OPERATION_FAILED,
                            "애플 로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
                })
                .blockOptional()
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.OPERATION_FAILED,
                        "애플 로그인 응답이 올바르지 않습니다."));
    }
}