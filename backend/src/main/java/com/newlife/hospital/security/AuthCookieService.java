package com.newlife.hospital.security;

import com.newlife.hospital.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Builds the session cookie: HttpOnly (no JS access), Secure, SameSite=Strict, scoped to the API. */
@Component
public class AuthCookieService {

    private final AppProperties.Jwt config;

    public AuthCookieService(AppProperties props) {
        this.config = props.jwt();
    }

    public ResponseCookie sessionCookie(String token, Instant expiresAt) {
        return base(token).maxAge(Duration.between(Instant.now(), expiresAt)).build();
    }

    public ResponseCookie clearedCookie() {
        return base("").maxAge(0).build();
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> config.cookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(config.cookieName(), value)
                .httpOnly(true)
                .secure(config.cookieSecure())
                .sameSite("Strict")
                .path("/api");
    }
}
