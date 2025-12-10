package com.dearwith.dearwith_backend.common.config;

import com.dearwith.dearwith_backend.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.OffsetDateTime;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://dearwith.kr",
                "https://www.dearwith.kr",
                "https://api.dearwith.kr"
        ));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oa -> oa.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint()) // 401 JSON
                        .accessDeniedHandler(accessDeniedHandler())    // 403 JSON
                )
                .authorizeHttpRequests(auth -> auth
                        // ===== 공개 =====
                        .requestMatchers(
                                "/",
                                "/auth/signin",
                                "/auth/signup/email/send",
                                "/auth/signup/email/verify",
                                "/auth/oauth/kakao",
                                "/auth/oauth/apple",
                                "/auth/validate",
                                "/auth/refresh",

                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/oauth2/**",

                                // 회원가입 / 비번 재설정 / 중복체크
                                "/users/signup",
                                "/users/signup/social",
                                "/users/check/**",
                                "/users/password/reset",

                                // 검색
                                "/api/search/artists",
                                "/api/search/artists/artists-groups",
                                "/api/places/**",

                                // 공지 조회
                                "/api/notices",
                                "/api/notices/*"
                        ).permitAll()

                        // ===== 조회용 공개 API (로그인 선택) =====
                        .requestMatchers(HttpMethod.GET,
                                // 메인
                                "/api/main",

                                // 이벤트
                                "/api/events",                 // 이벤트 목록
                                "/api/events/*",               // 이벤트 상세
                                "/api/events/*/reviews",       // 리뷰 목록
                                "/api/events/*/photoReviews",  // 포토리뷰 목록

                                // 리뷰 단건 조회
                                "/api/reviews/*",

                                // 이벤트 공지
                                "/api/events/*/notices",       // 특정 이벤트 공지 목록
                                "/api/events/notices/*",       // 공지 상세

                                // 아티스트 / 그룹
                                "/api/artists",                // 아티스트 목록
                                "/api/groups",                 // 그룹 목록
                                "/api/artists/*/events",       // 특정 아티스트의 이벤트 목록
                                "/api/groups/*/events"         // 특정 그룹의 이벤트 목록
                        ).permitAll()

                        // ===== 관리자 전용 API =====
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ===== 로그인 필수 =====
                        .requestMatchers(
                                // 내 정보 / 프로필 / 비번 변경
                                "/users/me",
                                "/users/me/**",
                                "/users/password/change",

                                // 로그 아웃
                                "/auth/logout",

                                // 업로드
                                "/api/uploads/*",

                                // 최근 검색어 (추가/조회/삭제)
                                "/api/search/recent",
                                "/api/search/recent/**",

                                // 마이 페이지
                                "/api/my",
                                "/api/my/**",

                                // 알림
                                "/api/notifications/**",

                                // 푸시 (디바이스 등록/해제, 테스트 포함)
                                "/api/push/**",

                                // 1:1 문의 (등록/내 목록/상세/만족도 선택)
                                "/api/inquiries",
                                "/api/inquiries/**",

                                // 디버그용 X 티켓 (안전하게 로그인 필수)
                                "/api/debug/**"
                        ).authenticated()

                        // ===== 쓰기/수정/삭제는 명시적으로 보호 =====

                        // 생성 계열
                        .requestMatchers(HttpMethod.POST,
                                "/api/events",                 // 이벤트 등록
                                "/api/events/*",
                                "/api/events/*/reviews",       // 리뷰 생성
                                "/api/events/*/notices",       // 공지 등록
                                "/api/reviews/*",              // 리뷰 신고 등 POST가 있다면
                                "/api/artists",                // 아티스트 등록
                                "/api/artists/*/bookmark",     // 아티스트 북마크 추가
                                "/api/artists/groups/*/bookmark" // 그룹 북마크 추가
                        ).authenticated()

                        // 수정(주로 PUT/PATCH)
                        .requestMatchers(HttpMethod.PUT,
                                "/api/artists/*",              // 아티스트 수정
                                "/api/groups/*"                // 아티스트 그룹 수정
                        ).authenticated()
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/events/*",               // 이벤트 수정
                                "/api/events/*/notices/*",     // 공지 수정
                                "/api/reviews/*"               // 리뷰 수정
                        ).authenticated()

                        // 삭제 계열
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/events/*",               // 이벤트 삭제
                                "/api/events/*/notices/*",     // 공지 삭제
                                "/api/reviews/*",              // 리뷰 삭제
                                "/api/artists/*/bookmark",     // 아티스트 북마크 해제
                                "/api/artists/groups/*/bookmark" // 그룹 북마크 해제
                        ).authenticated()

                        // 북마크 / 좋아요 / 신고 등 (GET/POST/DELETE 혼합)
                        .requestMatchers(
                                "/api/events/bookmark",          // 내 이벤트 북마크 목록
                                "/api/events/*/bookmark",        // 이벤트 북마크 토글
                                "/api/artists/bookmark",         // 북마크한 아티스트/그룹 조회
                                "/api/reviews/*/like",           // 리뷰 좋아요 추가/취소
                                "/api/reviews/*/report"          // 리뷰 신고
                        ).authenticated()

                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}