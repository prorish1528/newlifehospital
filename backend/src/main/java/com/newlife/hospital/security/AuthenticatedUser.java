package com.newlife.hospital.security;

import com.newlife.hospital.domain.Enums.Role;
import java.security.Principal;

/** The principal placed in the security context for an authenticated request. */
public record AuthenticatedUser(Long id, String username, Role role) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
