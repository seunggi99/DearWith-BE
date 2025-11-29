package com.dearwith.dearwith_backend.common.config;

import com.dearwith.dearwith_backend.common.component.SecurityAuditorAware;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

    @Bean
    public AuditorAware<User> auditorAware(UserRepository userRepository) {
        return new SecurityAuditorAware(userRepository);
    }
}
