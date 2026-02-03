package com.dearwith.dearwith_backend.notification.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PushRetryJob(
        @JsonProperty("tokens") List<String> tokens,
        @JsonProperty("title") String title,
        @JsonProperty("body") String body,
        @JsonProperty("url") String url,
        @JsonProperty("attempt") int attempt
) {
    @JsonCreator
    public PushRetryJob {
        // Compact constructor for validation
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("tokens cannot be empty");
        }
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1");
        }
    }

    public PushRetryJob withIncrementedAttempt() {
        return new PushRetryJob(tokens, title, body, url, attempt + 1);
    }
}
