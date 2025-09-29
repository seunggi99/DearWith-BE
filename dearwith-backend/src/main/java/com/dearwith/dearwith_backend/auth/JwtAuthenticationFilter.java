package com.dearwith.dearwith_backend.auth;

import com.dearwith.dearwith_backend.auth.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Order(1)  // SecurityFilterChain 보다 앞순서로 실행
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        final String jwtToken = authHeader.substring(7);

        try {
            UUID userId = tokenProvider.extractUserId(jwtToken);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (!tokenProvider.validateToken(jwtToken, userId)) {
                    unauthorized(response, "TOKEN_INVALID", "Invalid token");
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

        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            unauthorized(response, "TOKEN_EXPIRED", "Access token expired");
        } catch (MalformedJwtException | SignatureException | UnsupportedJwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            unauthorized(response, "TOKEN_INVALID", "Invalid token");
        } catch (JwtException e) { // 포괄
            SecurityContextHolder.clearContext();
            unauthorized(response, "TOKEN_INVALID", "Invalid token");
        }
    }

    private void unauthorized(HttpServletResponse resp, String code, String message) throws IOException {
        if (resp.isCommitted()) return;
        SecurityContextHolder.clearContext();

        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");

        String desc = switch (code) {
            case "TOKEN_EXPIRED" -> "The access token expired";
            case "TOKEN_INVALID" -> "The access token is invalid";
            default -> "Unauthorized";
        };
        resp.setHeader("WWW-Authenticate",
                "Bearer error=\"invalid_token\", error_description=\"" + desc + "\"");

        resp.getWriter().write("{\"message\":\"" + message + "\",\"code\":\"" + code + "\"}");
        resp.getWriter().flush();
    }
}