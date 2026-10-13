package com.newlife.hospital.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newlife.hospital.repository.UserRepository;
import com.newlife.hospital.security.AuthCookieService;
import com.newlife.hospital.security.JwtCookieAuthFilter;
import com.newlife.hospital.security.JwtService;
import com.newlife.hospital.security.SpaCsrfTokenRequestHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   AuthCookieService cookieService, UserRepository userRepository,
                                                   ObjectMapper objectMapper) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(c -> c.sameSite("Strict").path("/"));

        http
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .addFilterAfter(new SpaCsrfTokenRequestHandler.CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .cors(cors -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .headers(h -> h
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(f -> f.deny())
                    .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/public/**", "/api/validations/**", "/actuator/health").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/auth/me", "/api/auth/csrf").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/staff/**").hasRole("STAFF")
                    .anyRequest().authenticated())
            .exceptionHandling(e -> e
                    .authenticationEntryPoint((req, res, ex) ->
                            writeProblem(res, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                    "Your session has expired or you are not signed in."))
                    .accessDeniedHandler((req, res, ex) ->
                            writeProblem(res, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                    "You do not have permission to perform this action.")))
            .addFilterBefore(new JwtCookieAuthFilter(jwtService, cookieService, userRepository),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** CORS is only needed when the SPA is served from a different origin than the API (none in dev via proxy). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.cors() == null ? List.of() : props.cors().origins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void writeProblem(HttpServletResponse res, ObjectMapper mapper, int status, String detail)
            throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), Map.of("status", status, "detail", detail));
    }
}
