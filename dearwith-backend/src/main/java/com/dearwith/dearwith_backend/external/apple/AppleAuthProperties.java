package com.dearwith.dearwith_backend.external.apple;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth.apple")
public class AppleAuthProperties {


    private String teamId;
    private SignIn signin = new SignIn();
    private Push push = new Push();

    @Getter
    @Setter
    public static class SignIn {
        private String keyId;
        private String privateKey;
        private String clientId;
        private String tokenUrl;
        private String jwksUrl;
    }

    @Getter
    @Setter
    public static class Push {
        private String keyId;
        private String privateKey;
        private String topic;
    }

    public String getIssuer() {
        return teamId;
    }

    public String getAudience() {
        return "https://appleid.apple.com";
    }
}