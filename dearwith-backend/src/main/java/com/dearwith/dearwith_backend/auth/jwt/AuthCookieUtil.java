package com.dearwith.dearwith_backend.auth.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieUtil {

    private final AuthCookieProperties props;

    public void addAuthCookies(HttpServletResponse response,
                               String accessToken,
                               String refreshToken) {

        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", accessToken)
                .domain(props.getCookieDomain())
                .httpOnly(true)
                .secure(props.isCookieSecure())
                .sameSite(props.getCookieSameSite())
                .path("/")
                .maxAge(props.getExpirationTime())
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .domain(props.getCookieDomain())
                .httpOnly(true)
                .secure(props.isCookieSecure())
                .sameSite(props.getCookieSameSite())
                .path("/")
                .maxAge(props.getRefreshExpirationTime())
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    public void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}