package com.authfusion.sso.config;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.security.JwtAuthenticationFilter;
import com.authfusion.sso.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@ToeScope(value = "보안 필터 체인 구성", sfr = {"FIA_UAU.1", "FIA_UID.1", "FDP_ACC.1"})
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${authfusion.sso.cors.allowed-origins:http://localhost:3001}")
    private String allowedOrigins;

    @Value("${authfusion.sso.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${authfusion.sso.cors.max-age:3600}")
    private long maxAge;

    @Value("${authfusion.sso.cc.extended-features-enabled:true}")
    private boolean extendedEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> {
                    // OIDC public endpoints
                    auth.requestMatchers(
                            "/.well-known/**",
                            "/oauth2/authorize",
                            "/oauth2/token",
                            "/oauth2/revoke",
                            "/oauth2/userinfo",
                            "/oauth2/logout"
                    ).permitAll();

                    // Login page (both /login and /sso/login for Nginx reverse proxy)
                    auth.requestMatchers(
                            "/login",
                            "/login/**",
                            "/sso/login",
                            "/sso/mfa",
                            "/error"
                    ).permitAll();

                    // Static resources
                    auth.requestMatchers(
                            "/css/**",
                            "/js/**",
                            "/favicon.ico"
                    ).permitAll();

                    // Auth endpoints (login/logout - no token needed)
                    auth.requestMatchers("/api/v1/auth/**").permitAll();

                    // MFA endpoints (used during login flow)
                    auth.requestMatchers("/api/v1/mfa/**").permitAll();
                    auth.requestMatchers("/login/mfa").permitAll();

                    // CC 모드: 확장 기능 엔드포인트 차단
                    if (!extendedEnabled) {
                        auth.requestMatchers(
                                "/api/v1/clients/**",
                                "/api/v1/users/**",
                                "/api/v1/roles/**",
                                "/api/v1/sessions/**",
                                "/api/v1/audit/statistics",
                                "/api/v1/extensions/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/consent"
                        ).denyAll();
                    } else {
                        // 확장 모드: consent 페이지 허용
                        auth.requestMatchers("/consent").permitAll();

                        // Swagger
                        auth.requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**"
                        ).permitAll();
                    }

                    // Health endpoint (always available)
                    auth.requestMatchers("/actuator/health").permitAll();

                    if (extendedEnabled) {
                        auth.requestMatchers("/actuator/info", "/actuator/metrics").permitAll();
                    }

                    // Audit events query (TOE - available in CC mode too)
                    auth.requestMatchers("/api/v1/audit/events").authenticated();

                    // All other API endpoints require authentication (Bearer Token)
                    auth.requestMatchers("/api/v1/**").authenticated();
                    auth.anyRequest().authenticated();
                })
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        config.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "Accept", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
