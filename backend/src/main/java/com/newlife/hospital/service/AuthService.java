package com.newlife.hospital.service;

import com.newlife.hospital.domain.User;
import com.newlife.hospital.exception.ApiException;
import com.newlife.hospital.repository.StaffProfileRepository;
import com.newlife.hospital.repository.UserRepository;
import com.newlife.hospital.security.AuthenticatedUser;
import com.newlife.hospital.security.JwtService;
import com.newlife.hospital.security.JwtService.IssuedToken;
import com.newlife.hospital.security.LoginAttemptService;
import com.newlife.hospital.validation.DynamicValidator;
import com.newlife.hospital.web.dto.AuthDtos.ChangePasswordRequest;
import com.newlife.hospital.web.dto.AuthDtos.LoginRequest;
import com.newlife.hospital.web.dto.AuthDtos.SessionUser;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS = "Invalid username or password.";

    public record LoginResult(SessionUser user, IssuedToken token) {}

    private final UserRepository users;
    private final StaffProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService attempts;
    private final DynamicValidator validator;
    /** Compared against when the username does not exist, so response time does not reveal valid usernames. */
    private final String dummyHash;

    public AuthService(UserRepository users, StaffProfileRepository profiles, PasswordEncoder passwordEncoder,
                       JwtService jwtService, LoginAttemptService attempts, DynamicValidator validator) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.attempts = attempts;
        this.validator = validator;
        this.dummyHash = passwordEncoder.encode("timing-equalisation-placeholder");
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        validator.form("login")
                .check("username", request.username())
                .check("password", request.password())
                .throwIfInvalid();
        if (request.portal() == null) {
            throw ApiException.badRequest("Choose the Staff or Admin portal to sign in.");
        }
        String username = Text.lower(request.username());
        if (attempts.isLocked(username)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed sign-in attempts. Please try again in a few minutes.");
        }

        User user = users.findByUsername(username).orElse(null);
        boolean passwordOk = passwordEncoder.matches(request.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !passwordOk || !user.isEnabled()) {
            attempts.recordFailure(username);
            log.info("Failed sign-in for username '{}'", username);
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        if (user.getRole() != request.portal()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account cannot sign in to the "
                    + request.portal().name().toLowerCase() + " portal. Use the " + user.getRole().name().toLowerCase()
                    + " portal instead.");
        }
        attempts.recordSuccess(username);
        IssuedToken token = jwtService.issue(user);
        log.info("User '{}' signed in ({})", username, user.getRole());
        return new LoginResult(sessionUser(user, token.expiresAt()), token);
    }

    @Transactional(readOnly = true)
    public SessionUser me(AuthenticatedUser principal) {
        User user = users.findById(principal.id()).orElseThrow(() -> ApiException.notFound("User"));
        return sessionUser(user, null);
    }

    @Transactional(readOnly = true)
    public String displayName(AuthenticatedUser principal) {
        return profiles.findByUserId(principal.id()).map(p -> p.getFullName()).orElse(principal.username());
    }

    /** Revokes every outstanding session of the user (all devices). */
    @Transactional
    public void logout(AuthenticatedUser principal) {
        users.findById(principal.id()).ifPresent(User::revokeSessions);
    }

    /** Changes the caller's password, revokes their old sessions and returns a fresh token. */
    @Transactional
    public LoginResult changePassword(AuthenticatedUser principal, ChangePasswordRequest request) {
        User user = users.findById(principal.id()).orElseThrow(() -> ApiException.notFound("User"));
        var check = validator.form("staff").check("newPassword", "password", request.newPassword());
        if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            check.reject("currentPassword", "Current password is incorrect.");
        } else if (request.currentPassword().equals(request.newPassword())) {
            check.reject("newPassword", "New password must be different from the current password.");
        }
        check.throwIfInvalid();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.revokeSessions();
        users.flush();
        IssuedToken token = jwtService.issue(user);
        return new LoginResult(sessionUser(user, token.expiresAt()), token);
    }

    private SessionUser sessionUser(User user, Instant expiresAt) {
        String fullName = profiles.findByUserId(user.getId()).map(p -> p.getFullName()).orElse(
                user.getRole().name().equals("ADMIN") ? "Administrator" : user.getUsername());
        return new SessionUser(user.getUsername(), user.getRole(), fullName, expiresAt);
    }
}
