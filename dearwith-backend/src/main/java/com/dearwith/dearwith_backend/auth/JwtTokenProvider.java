package com.dearwith.dearwith_backend.auth;

import com.dearwith.dearwith_backend.auth.dto.TokenCreateRequestDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;


import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

@Component
public class JwtTokenProvider {
    private SecretKey key;

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expiration-time}")
    private long expirationTime;

    @Value("${jwt.refresh-expiration-time}")
    private long refreshExpirationTime;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }


    // 일반 토큰 생성
    public String generateToken(TokenCreateRequestDto request) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", request.getUserId());
        claims.put("email", request.getEmail());
        claims.put("role", request.getRole());

        return buildToken(claims, request.getEmail(), expirationTime);
    }

    // 리프레시 토큰 생성
    public String generateRefreshToken(TokenCreateRequestDto request) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", request.getUserId());
        claims.put("email", request.getEmail());
        claims.put("role", request.getRole());

        return buildToken(claims, request.getEmail(), refreshExpirationTime);
    }

    // 토큰 생성 내부 로직 (중복 제거)
    private String buildToken(Map<String, Object> claims, String subject, long expireMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Claims 추출
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 개별 Claim 추출 (람다 적용)
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    // 이메일(=subject) 추출
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // userId 추출
    public UUID extractUserId(String token) {
        Object value = extractAllClaims(token).get("userId");
        if (value instanceof String) return UUID.fromString((String)value);
        return (UUID) value;
    }
    // role 추출
    public String extractRole(String token) {
        Object value = extractAllClaims(token).get("role");
        return value != null ? value.toString() : null;
    }

    // 만료 체크
    public boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token, UUID userId) {
        UUID tokenUserId = extractUserId(token);
        return (tokenUserId.equals(userId) && !isTokenExpired(token));
    }

}
