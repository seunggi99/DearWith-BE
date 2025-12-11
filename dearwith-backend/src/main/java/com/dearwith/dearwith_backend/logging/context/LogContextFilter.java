package com.dearwith.dearwith_backend.logging.context;

import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class LogContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String traceId = resolveTraceId(request);
            String remoteIp = resolveRemoteIp(request);
            String userAgent = request.getHeader("User-Agent");
            UUID userId = resolveCurrentUserId();

            LogContext context = LogContext.builder()
                    .traceId(traceId)
                    .requestUri(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .remoteIp(remoteIp)
                    .userAgent(userAgent)
                    .userId(userId)
                    .build();

            LogContextHolder.set(context);

            filterChain.doFilter(request, response);
        } finally {
            LogContextHolder.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String fromHeader = request.getHeader("X-Trace-Id");
        return StringUtils.hasText(fromHeader) ? fromHeader : UUID.randomUUID().toString();
    }

    private String resolveRemoteIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xf)) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private UUID resolveCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userPrincipal) {
            return userPrincipal.getId();
        }

        return null;
    }
}