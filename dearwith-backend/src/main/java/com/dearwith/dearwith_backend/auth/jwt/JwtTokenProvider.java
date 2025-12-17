package com.dearwith.dearwith_backend.auth.jwt;

import com.dearwith.dearwith_backend.auth.dto.TokenCreateRequestDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYP = "typ";

    public static final String TYP_ACCESS = "ACCESS";
    public static final String TYP_REFRESH = "REFRESH";

    private SecretKey key;

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expiration-time}")
    private long expirationTime; // ms

    @Value("${jwt.refresh-expiration-time-web}")
    private long refreshExpirationTimeWeb; // ms

    @Value("${jwt.refresh-expiration-time-app}")
    private long refreshExpirationTimeApp; // ms

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    /* =========================
     *  Token 생성
     * ========================= */

    // ACCESS 토큰
    public String generateAccessToken(TokenCreateRequestDto dto) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, dto.getUserId().toString());
        claims.put(CLAIM_EMAIL, dto.getEmail());
        claims.put(CLAIM_ROLE, dto.getRole());
        claims.put(CLAIM_TYP, TYP_ACCESS);

        return buildToken(claims, dto.getEmail(), expirationTime, null);
    }

    // REFRESH 토큰 (웹/앱 TTL 분리)
    public String generateRefreshTokenWeb(TokenCreateRequestDto dto) {
        return generateRefreshToken(dto, refreshExpirationTimeWeb);
    }

    public String generateRefreshTokenApp(TokenCreateRequestDto dto) {
        return generateRefreshToken(dto, refreshExpirationTimeApp);
    }

    private String generateRefreshToken(TokenCreateRequestDto dto, long refreshTtlMillis) {
        String jti = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, dto.getUserId().toString());
        claims.put(CLAIM_EMAIL, dto.getEmail());
        claims.put(CLAIM_ROLE, dto.getRole());
        claims.put(CLAIM_TYP, TYP_REFRESH);

        return buildToken(claims, dto.getEmail(), refreshTtlMillis, jti);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expireMillis, String jti) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMillis))
                .claims(claims)
                .signWith(key);

        if (jti != null) builder.id(jti);

        return builder.compact();
    }

    /* =========================
     *  파싱/검증
     * ========================= */

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    public UUID extractUserId(String token) {
        String v = parseClaims(token).get(CLAIM_USER_ID, String.class);
        return v == null ? null : UUID.fromString(v);
    }

    public String extractTyp(String token) {
        return parseClaims(token).get(CLAIM_TYP, String.class);
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public long getRefreshTokenTtlSecondsWeb() {
        return refreshExpirationTimeWeb / 1000L;
    }

    public long getRefreshTokenTtlSecondsApp() {
        return refreshExpirationTimeApp / 1000L;
    }
}