package com.dearwith.dearwith_backend.external.x;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class TicketCacheConfig {

    @Bean
    public Cache<String, XVerifyPayload> xVerifyTicketCache(
            @Value("${x.verify.ticket.ttl-minutes:30}") long ttlMinutes,
            @Value("${x.verify.ticket.max-size:10000}") long maxSize
    ) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .build();
    }
}
