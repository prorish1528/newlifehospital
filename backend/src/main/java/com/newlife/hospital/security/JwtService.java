package com.newlife.hospital.security;

import com.newlife.hospital.config.AppProperties;
import com.newlife.hospital.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and verifies HS256-signed session tokens. Tokens travel only inside an HttpOnly cookie. */
@Service
public class JwtService {

    private static final String ISSUER = "newlife-hospital";

    private final SecretKey key;
    private final AppProperties.Jwt config;

    public JwtService(AppProperties props) {
        this.config = props.jwt();
        byte[] secret = Base64.getDecoder().decode(config.secret().trim());
        if (secret.length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must decode to at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public record IssuedToken(String value, Instant expiresAt) {}

    public record TokenClaims(String username, String role, int tokenVersion) {}

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(config.expiration());
        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getUsername())
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .claim("ver", user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    public Optional<TokenClaims> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Integer version = claims.get("ver", Integer.class);
            String role = claims.get("role", String.class);
            if (version == null || role == null || claims.getSubject() == null) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(claims.getSubject(), role, version));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
