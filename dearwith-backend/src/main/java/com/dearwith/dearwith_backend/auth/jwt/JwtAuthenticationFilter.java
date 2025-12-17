package com.dearwith.dearwith_backend.auth.jwt;

import com.dearwith.dearwith_backend.auth.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TYPE = "ACCESS";

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String jwtToken = resolveAccessToken(request);

        if (jwtToken == null || jwtToken.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = tokenProvider.parseClaims(jwtToken);

            String typ = claims.get("typ", String.class);
            if (!JwtTokenProvider.TYP_ACCESS.equals(typ)) {
                request.setAttribute("auth_error", "TOKEN_WRONG_TYPE");
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            String userIdStr = claims.get("userId", String.class);
            UUID userId = (userIdStr == null) ? null : UUID.fromString(userIdStr);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserById(userId);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }

            chain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            request.setAttribute("auth_error", "TOKEN_EXPIRED");
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException e) {
            request.setAttribute("auth_error", "TOKEN_INVALID");
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);

        } catch (UsernameNotFoundException e) {
            request.setAttribute("auth_error", "USER_NOT_FOUND");
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
        }
    }

    private String resolveAccessToken(HttpServletRequest request) {
        // 1) Authorization 헤더 우선
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isBlank()) return token;
        }

        // 2) 쿠키 fallback
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("ACCESS_TOKEN".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.isBlank()) return value;
                }
            }
        }

        return null;
    }
}