package com.dearwith.dearwith_backend.auth.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@Setter
@ConfigurationProperties(prefix = "jwt")
public class AuthCookieProperties {
    private boolean cookieSecure;
    private String cookieSameSite;
    private long expirationTime;
    private long refreshExpirationTime;
}
