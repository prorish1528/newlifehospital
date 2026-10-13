package com.newlife.hospital.repository;

import com.newlife.hospital.domain.Enums.Role;
import com.newlife.hospital.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);
}
