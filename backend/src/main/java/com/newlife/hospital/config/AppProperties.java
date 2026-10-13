package com.newlife.hospital.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Strongly typed view of the {@code app.*} configuration tree. */
@ConfigurationProperties("app")
public record AppProperties(
        @DefaultValue("Asia/Kolkata") ZoneId zone,
        Jwt jwt,
        Crypto crypto,
        BootstrapAdmin bootstrapAdmin,
        DevSeed devSeed,
        @DefaultValue("./uploads") String uploadDir,
        Cors cors,
        Login login) {

    public record Jwt(String secret,
                      @DefaultValue("PT8H") Duration expiration,
                      @DefaultValue("NLH_SESSION") String cookieName,
                      @DefaultValue("true") boolean cookieSecure) {}

    public record Crypto(String key, String hmacKey) {}

    public record BootstrapAdmin(String username, String password) {}

    public record DevSeed(String staffUsername, String staffPassword) {}

    public record Cors(List<String> allowedOrigins) {
        public List<String> origins() {
            return allowedOrigins == null ? List.of()
                    : allowedOrigins.stream().filter(o -> o != null && !o.isBlank()).map(String::trim).toList();
        }
    }

    public record Login(@DefaultValue("5") int maxAttempts, @DefaultValue("PT15M") Duration lockDuration) {}
}
