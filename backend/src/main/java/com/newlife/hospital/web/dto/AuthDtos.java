package com.newlife.hospital.web.dto;

import com.newlife.hospital.domain.Enums.Role;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    /** {@code portal} is the sign-in view used; an account may only sign in to the portal matching its role. */
    public record LoginRequest(String username, String password, Role portal) {}

    public record SessionUser(String username, Role role, String fullName, Instant expiresAt) {}

    public record ChangePasswordRequest(String currentPassword, String newPassword) {}
}
