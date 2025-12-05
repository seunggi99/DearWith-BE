package com.dearwith.dearwith_backend.auth.jwt;

import com.dearwith.dearwith_backend.auth.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1) 쿠키에서 ACCESS_TOKEN 추출
        String jwtToken = resolveAccessToken(request);

        // 토큰 없으면 비로그인 요청으로 처리
        if (jwtToken == null || jwtToken.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            UUID userId = tokenProvider.extractUserId(jwtToken);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (!tokenProvider.validateToken(jwtToken)) {
                    chain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserById(userId);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }

            chain.doFilter(request, response);

        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            log.warn("Invalid JWT token: {}", e.getMessage());
            chain.doFilter(request, response);
        } catch (UsernameNotFoundException ex) {
            log.warn("JWT 유저를 찾을 수 없습니다. 토큰 무시: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        // 1) Authorization 헤더 우선 (Swagger용, 외부 툴용)
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        // 2) 쿠키 (실제 웹/앱에서 사용하는 경로)
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("ACCESS_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String resolveAccessToken(HttpServletRequest request) {
        // 1) 쿠키 우선
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("ACCESS_TOKEN".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                }
            }
        }

        // 2) Authorization 헤더 (Bearer 토큰)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!token.isBlank()) {
                return token;
            }
        }

        return null;
    }
}