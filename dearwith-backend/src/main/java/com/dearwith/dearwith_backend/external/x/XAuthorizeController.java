package com.dearwith.dearwith_backend.external.x;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
public class XAuthorizeController {

    private final XAuthProps props;
    private final XAuthService xAuthService;
    private final XVerifyTicketService ticketService;

    private final Map<String, String> verifierStore = new ConcurrentHashMap<>();

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String FRONT;

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
                .queryParam("scope", "users.read%20tweet.read%20offline.access")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build(true).toUriString();

        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    @GetMapping("/oauth2/callback/x")
    public ResponseEntity<Map<String, Object>> callback(
            @CurrentUser UUID userId,
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest req
    ) {
        String redirectUri = resolveRedirectUri(req);

        String codeVerifier = verifierStore.remove(state);
        if (codeVerifier == null) {
            return ResponseEntity.status(302)
                    .location(URI.create(FRONT + "/organizer/verify?result=error&reason=state"))
                    .build();
        }

        var me = xAuthService.verifyWithCode(redirectUri, code, codeVerifier);

        String ticket = ticketService.issueTicket(
                userId,
                me.data().id(),
                me.data().username(),
                me.data().name(),
                true
        );

        String url = UriComponentsBuilder.fromHttpUrl(FRONT + "/organizer/verify")
                .queryParam("result", "success")
                .queryParam("ticket", ticket)
                .queryParam("handle", me.data().username())
                .build(true).toUriString();

        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    @GetMapping("/api/debug/x-ticket/{ticket}")
    @Operation(summary = "X(트위터) 인증 티켓 테스트용")
    public ResponseEntity<?> checkTicket(@PathVariable String ticket) {
        var payload = ticketService.peek(ticket);
        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "티켓이 없거나 만료됨"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "xId", payload.xId(),
                "xHandle", payload.xHandle(),
                "xName", payload.xName(),
                "verified", payload.verified(),
                "issuedAt", payload.issuedAt().toString()
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
