package com.dearwith.dearwith_backend.auth.jwt;

import com.dearwith.dearwith_backend.auth.dto.TokenCreateRequestDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

    // ACCESS 토큰
    public String generateToken(TokenCreateRequestDto request) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", request.getUserId().toString());
        claims.put("email", request.getEmail());
        claims.put("role", request.getRole());
        claims.put("typ", "ACCESS");

        return buildToken(claims, request.getEmail(), expirationTime);
    }

    // REFRESH 토큰 (웹/앱 TTL 분리)
    public String generateRefreshTokenWeb(TokenCreateRequestDto dto) {
        return generateRefreshToken(dto, refreshExpirationTimeWeb);
    }

    public String generateRefreshTokenApp(TokenCreateRequestDto dto) {
        return generateRefreshToken(dto, refreshExpirationTimeApp);
    }

    private String generateRefreshToken(TokenCreateRequestDto dto, long refreshTtlMillis) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTtlMillis);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(dto.getEmail())
                .id(jti)
                .issuedAt(now)
                .expiration(exp)
                .claim("userId", dto.getUserId().toString())
                .claim("email", dto.getEmail())
                .claim("role", dto.getRole())
                .claim("typ", "REFRESH")
                .signWith(key)
                .compact();
    }

    private String buildToken(Map<String, Object> claims, String subject, long expireMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMillis))
                .claims(claims)
                .signWith(key)
                .compact();
    }

    // Claims 추출 (✅ 최신 parser)
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public UUID extractUserId(String token) {
        Object value = extractAllClaims(token).get("userId");
        return value == null ? null : UUID.fromString(value.toString());
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean validateToken(String token) {
        try {
            Date exp = extractExpiration(token);
            return exp != null && exp.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            Object typ = extractAllClaims(token).get("typ");
            return "ACCESS".equals(String.valueOf(typ));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Object typ = extractAllClaims(token).get("typ");
            return "REFRESH".equals(String.valueOf(typ));
        } catch (Exception e) {
            return false;
        }
    }

    public long getRefreshTokenTtlSecondsWeb() {
        return refreshExpirationTimeWeb / 1000L;
    }

    public long getRefreshTokenTtlSecondsApp() {
        return refreshExpirationTimeApp / 1000L;
    }
}