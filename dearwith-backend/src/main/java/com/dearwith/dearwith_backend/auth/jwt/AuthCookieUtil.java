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
                .httpOnly(true)
                .secure(props.isCookieSecure())
                .sameSite(props.getCookieSameSite())
                .path("/")
                .maxAge(props.getExpirationTime() / 1000)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .httpOnly(true)
                .secure(props.isCookieSecure())
                .sameSite(props.getCookieSameSite())
                .path("/")
                .maxAge(props.getRefreshExpirationTimeWeb() / 1000)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    public void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cleared = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(props.isCookieSecure())
                .sameSite(props.getCookieSameSite())
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cleared.toString());
    }

}