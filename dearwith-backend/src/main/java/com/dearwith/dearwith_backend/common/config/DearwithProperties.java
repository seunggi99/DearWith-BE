package com.dearwith.dearwith_backend.common.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "dearwith")
public class DearwithProperties {
    private String baseUrl;
}