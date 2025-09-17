package com.dearwith.dearwith_backend.external.x;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
public class XAuthorizeController {

    private final XAuthProps props;
    private final XAuthService xAuthService;
    private final Map<String, String> verifierStore = new ConcurrentHashMap<>();

    @GetMapping("/oauth2/x/authorize")
    @Operation(summary = "X(트위터) 인증")
    public ResponseEntity<Void> authorize(HttpServletRequest req) {
        String redirectUri = resolveRedirectUri(req);
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        String state = "dw-" + System.currentTimeMillis();
        verifierStore.put(state, codeVerifier);

        String url = UriComponentsBuilder
                .fromHttpUrl("https://twitter.com/i/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", props.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "users.read%20tweet.read%20offline.access")                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build(true).toUriString();

        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    @GetMapping("/oauth2/callback/x")
    public ResponseEntity<Map<String, Object>> callback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest req
    ) {
        String redirectUri = resolveRedirectUri(req);

        String codeVerifier = verifierStore.remove(state);
        if (codeVerifier == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid state or expired session"));
        }

        var me = xAuthService.verifyWithCode(redirectUri, code, codeVerifier);

        return ResponseEntity.ok(Map.of(
                "verified", true,
                "twitterId", me.data().id(),
                "twitterHandle", me.data().username(),
                "twitterName", me.data().name()
        ));
    }



    private String resolveRedirectUri(HttpServletRequest req) {
        String scheme = Optional.ofNullable(req.getHeader("X-Forwarded-Proto")).orElse(req.getScheme());
        String host   = Optional.ofNullable(req.getHeader("X-Forwarded-Host")).orElse(req.getServerName());
        String base   = scheme + "://" + host;
        int port = req.getServerPort();
        if (req.getHeader("X-Forwarded-Host") == null && port != 80 && port != 443) base += ":" + port;
        return base + "/oauth2/callback/x";
    }

    private String generateCodeVerifier() {
        byte[] code = new byte[32];
        new SecureRandom().nextBytes(code);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(code);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code_challenge", e);
        }
    }

}
