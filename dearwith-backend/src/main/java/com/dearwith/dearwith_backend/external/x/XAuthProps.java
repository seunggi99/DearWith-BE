package com.dearwith.dearwith_backend.external.x;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.x-twitter")
@Getter
@Setter
public class XAuthProps {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}