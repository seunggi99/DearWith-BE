package com.dearwith.dearwith_backend.common.config;

import com.dearwith.dearwith_backend.auth.resolver.CurrentUserArgumentResolver;
import com.dearwith.dearwith_backend.logging.context.LogContextFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
    @Bean
    public FilterRegistrationBean<LogContextFilter> logContextFilter() {
        FilterRegistrationBean<LogContextFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LogContextFilter());
        registrationBean.setOrder(1);
        return registrationBean;
    }
}