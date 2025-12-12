package com.dearwith.dearwith_backend.common.config;

import com.dearwith.dearwith_backend.artist.dto.MonthlyAnniversaryCacheDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {

        ObjectMapper om = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        GenericJackson2JsonRedisSerializer defaultSerializer =
                new GenericJackson2JsonRedisSerializer(om);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(defaultSerializer))
                .disableCachingNullValues()
                .entryTtl(Duration.ofHours(24));

        Jackson2JsonRedisSerializer<MonthlyAnniversaryCacheDto> annivSerializer =
                new Jackson2JsonRedisSerializer<>(om, MonthlyAnniversaryCacheDto.class);

        RedisCacheConfiguration annivConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(annivSerializer))
                .disableCachingNullValues()
                .entryTtl(Duration.ofHours(24));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("thisMonthAnniversaries", annivConfig)
                .build();
    }
}