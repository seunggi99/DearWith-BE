package com.dearwith.dearwith_backend.common.config;

import com.dearwith.dearwith_backend.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.OffsetDateTime;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UserDetailsService userDetailsService;

    @Bean
    public AuthenticationEntryPoint jwtAuthEntryPoint() {
        return (req, res, ex) -> {
            res.setStatus(401);
            res.setContentType("application/json;charset=UTF-8");
            String body = """
                {"timestamp":"%s","status":401,"error":"Unauthorized","message":"로그인이 필요합니다.","path":"%s"}
                """.formatted(OffsetDateTime.now(), req.getRequestURI());
            res.getWriter().write(body);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (req, res, ex) -> {
            res.setStatus(403);
            res.setContentType("application/json;charset=UTF-8");
            String body = """
                {"timestamp":"%s","status":403,"error":"Forbidden","message":"접근 권한이 없습니다.","path":"%s"}
                """.formatted(OffsetDateTime.now(), req.getRequestURI());
            res.getWriter().write(body);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oa -> oa.disable())

                // 미인증은 무조건 401로 깔끔히: 익명 비활성화(선택이지만 권장)
                .anonymous(anon -> anon.disable())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint()) // 401 JSON
                        .accessDeniedHandler(accessDeniedHandler())    // 403 JSON
                )

                .authorizeHttpRequests(auth -> auth
                        // ===== 공개 =====
                        .requestMatchers(
                                "/auth/**",
                                "/users/all",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // ===== 로그인 필수 (X 인증 플로우 등) =====
                        .requestMatchers(
                                "/users/me/**",
                                "/api/main",
                                "/api/events/*/bookmark",
                                "/api/uploads/**"
                            //    "/oauth2/x/authorize",
                            //    "/oauth2/callback/x",
                            //    "/api/events/organizer/verify-x"
                        ).authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/events").authenticated()
                        // 개발 중 나머지
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}