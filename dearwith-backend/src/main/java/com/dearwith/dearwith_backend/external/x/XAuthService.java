package com.dearwith.dearwith_backend.external.x;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class XAuthService {

    private final RestClient rest = RestClient.create();
    private final XAuthProps props;

    public XUserMe verifyWithCode(String absoluteRedirectUri, String code, String codeVerifier) {
        var token = exchangeToken(absoluteRedirectUri, code, codeVerifier);
        return fetchMe(token.access_token());
    }

    private TokenResponse exchangeToken(String redirectUri, String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", props.getClientId());
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("code_verifier", codeVerifier);

        String credentials = props.getClientId() + ":" + props.getClientSecret();
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return rest.post()
                .uri("https://api.x.com/2/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, basicAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }

    private XUserMe fetchMe(String accessToken) {
        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host("api.x.com").path("/2/users/me")
                        .queryParam("user.fields", "name,username,created_at").build())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(XUserMe.class);
    }

    public record TokenResponse(String token_type, String access_token,
                                Long expires_in, String refresh_token, String scope) {}

    public record XUserMe(Data data) {
        public record Data(String id, String name, String username) {}
    }
}
