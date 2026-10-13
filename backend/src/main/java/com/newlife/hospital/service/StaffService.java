package com.newlife.hospital.service;

import com.newlife.hospital.domain.Enums.Role;
import com.newlife.hospital.domain.StaffProfile;
import com.newlife.hospital.domain.User;
import com.newlife.hospital.exception.ApiException;
import com.newlife.hospital.repository.StaffProfileRepository;
import com.newlife.hospital.repository.UserRepository;
import com.newlife.hospital.validation.DynamicValidator;
import com.newlife.hospital.web.dto.AdminDtos.CreateStaffRequest;
import com.newlife.hospital.web.dto.AdminDtos.ResetPasswordRequest;
import com.newlife.hospital.web.dto.AdminDtos.ResetUsernameRequest;
import com.newlife.hospital.web.dto.AdminDtos.StaffView;
import com.newlife.hospital.web.dto.AdminDtos.UpdateStaffRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin-only management of STAFF login accounts. Admin accounts cannot be modified through these operations. */
@Service
public class StaffService {

    private static final Logger log = LoggerFactory.getLogger(StaffService.class);

    private final UserRepository users;
    private final StaffProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final DynamicValidator validator;

    public StaffService(UserRepository users, StaffProfileRepository profiles, PasswordEncoder passwordEncoder,
                        DynamicValidator validator) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<StaffView> list() {
        return profiles.findAllByRole(Role.STAFF).stream().map(Mappers::staff).toList();
    }

    @Transactional
    public StaffView create(CreateStaffRequest req, String adminUsername) {
        String username = Text.lower(req.username());
        String fullName = Text.clean(req.fullName());
        String email = Text.lower(req.email());
        String phone = Text.clean(req.phoneNumber());
        var check = validator.form("staff")
                .check("username", username)
                .check("password", req.password())
                .check("fullName", fullName)
                .check("email", email)
                .check("phoneNumber", phone);
        if (!check.hasError("username") && users.existsByUsername(username)) {
            check.reject("username", "This username is already taken.");
        }
        check.throwIfInvalid();

        User user = users.save(new User(username, passwordEncoder.encode(req.password()), Role.STAFF));
        StaffProfile profile = profiles.save(new StaffProfile(user, fullName, email, phone));
        log.info("Admin '{}' created staff account '{}'", adminUsername, username);
        return Mappers.staff(profile);
    }

    @Transactional
    public StaffView update(Long userId, UpdateStaffRequest req, String adminUsername) {
        StaffProfile profile = findStaff(userId);
        String fullName = Text.clean(req.fullName());
        String email = Text.lower(req.email());
        String phone = Text.clean(req.phoneNumber());
        validator.form("staff")
                .check("fullName", fullName)
                .check("email", email)
                .check("phoneNumber", phone)
                .throwIfInvalid();
        profile.setFullName(fullName);
        profile.setEmail(email);
        profile.setPhoneNumber(phone);
        if (req.enabled() != null && req.enabled() != profile.getUser().isEnabled()) {
            profile.getUser().setEnabled(req.enabled());
            profile.getUser().revokeSessions();
            log.info("Admin '{}' {} staff account '{}'", adminUsername, req.enabled() ? "enabled" : "disabled",
                    profile.getUser().getUsername());
        }
        return Mappers.staff(profile);
    }

    @Transactional
    public void delete(Long userId, String adminUsername) {
        StaffProfile profile = findStaff(userId);
        User user = profile.getUser();
        profiles.delete(profile);
        users.delete(user);
        log.info("Admin '{}' deleted staff account '{}'", adminUsername, user.getUsername());
    }

    @Transactional
    public void resetPassword(Long userId, ResetPasswordRequest req, String adminUsername) {
        StaffProfile profile = findStaff(userId);
        validator.form("staff").check("newPassword", "password", req.newPassword()).throwIfInvalid();
        User user = profile.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.revokeSessions();
        log.info("Admin '{}' reset the password of '{}'", adminUsername, user.getUsername());
    }

    @Transactional
    public StaffView resetUsername(Long userId, ResetUsernameRequest req, String adminUsername) {
        StaffProfile profile = findStaff(userId);
        String username = Text.lower(req.newUsername());
        var check = validator.form("staff").check("newUsername", "username", username);
        User user = profile.getUser();
        if (!check.hasError("newUsername") && !username.equals(user.getUsername()) && users.existsByUsername(username)) {
            check.reject("newUsername", "This username is already taken.");
        }
        check.throwIfInvalid();
        String old = user.getUsername();
        user.setUsername(username);
        user.revokeSessions();
        log.info("Admin '{}' renamed staff account '{}' to '{}'", adminUsername, old, username);
        return Mappers.staff(profile);
    }

    private StaffProfile findStaff(Long userId) {
        return profiles.findByUserId(userId)
                .filter(p -> p.getUser().getRole() == Role.STAFF)
                .orElseThrow(() -> ApiException.notFound("Staff account"));
    }
}
