package com.newlife.hospital.security;

import com.newlife.hospital.config.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Brute-force protection: after {@code maxAttempts} consecutive failures a username is locked for
 * {@code lockDuration}. In-memory, so it is per instance; use a shared store (e.g. Redis) when scaling out.
 */
@Service
public class LoginAttemptService {

    private record Attempts(int failures, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final AppProperties.Login config;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(AppProperties props) {
        this(props, Clock.systemUTC());
    }

    LoginAttemptService(AppProperties props, Clock clock) {
        this.config = props.login();
        this.clock = clock;
    }

    public boolean isLocked(String username) {
        Attempts a = attempts.get(key(username));
        return a != null && a.lockedUntil() != null && a.lockedUntil().isAfter(clock.instant());
    }

    public void recordFailure(String username) {
        attempts.compute(key(username), (k, current) -> {
            Instant now = clock.instant();
            int failures = (current == null || isExpiredLock(current, now)) ? 1 : current.failures() + 1;
            Instant lockedUntil = failures >= config.maxAttempts() ? now.plus(config.lockDuration()) : null;
            return new Attempts(failures, lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private static boolean isExpiredLock(Attempts a, Instant now) {
        return a.lockedUntil() != null && !a.lockedUntil().isAfter(now);
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
