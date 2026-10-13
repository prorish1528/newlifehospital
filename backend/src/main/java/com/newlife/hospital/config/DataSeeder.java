package com.newlife.hospital.config;

import com.newlife.hospital.domain.Enums.Role;
import com.newlife.hospital.domain.StaffProfile;
import com.newlife.hospital.domain.User;
import com.newlife.hospital.repository.StaffProfileRepository;
import com.newlife.hospital.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator from {@code APP_ADMIN_USERNAME}/{@code APP_ADMIN_PASSWORD} when no admin
 * exists yet, and (dev profile only) a demo staff account. Passwords are BCrypt-hashed and never logged.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final StaffProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public DataSeeder(UserRepository users, StaffProfileRepository profiles, PasswordEncoder passwordEncoder,
                      AppProperties props) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var admin = props.bootstrapAdmin();
        if (!users.existsByRole(Role.ADMIN)) {
            if (admin == null || isBlank(admin.username()) || isBlank(admin.password())) {
                log.warn("No administrator exists. Set APP_ADMIN_USERNAME and APP_ADMIN_PASSWORD and restart.");
            } else if (admin.password().length() < 10) {
                log.error("APP_ADMIN_PASSWORD must be at least 10 characters; administrator not created.");
            } else {
                users.save(new User(admin.username().trim().toLowerCase(), passwordEncoder.encode(admin.password()),
                        Role.ADMIN));
                log.info("Bootstrap administrator '{}' created.", admin.username());
            }
        }

        var seed = props.devSeed();
        if (seed != null && !isBlank(seed.staffUsername()) && !isBlank(seed.staffPassword())
                && !users.existsByUsername(seed.staffUsername())) {
            User staff = users.save(new User(seed.staffUsername(), passwordEncoder.encode(seed.staffPassword()), Role.STAFF));
            profiles.save(new StaffProfile(staff, "Demo Staff", null, null));
            log.warn("DEV seed: demo staff account '{}' created. Do not enable dev-seed outside local development.",
                    seed.staffUsername());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
