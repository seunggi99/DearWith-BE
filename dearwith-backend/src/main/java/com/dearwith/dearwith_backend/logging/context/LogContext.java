package com.dearwith.dearwith_backend.logging.context;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class LogContext {

    private final String traceId;

    private final UUID userId;

    private final String requestUri;

    private final String httpMethod;

    private final String remoteIp;

    private final String userAgent;
}