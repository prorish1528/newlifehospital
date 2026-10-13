package com.newlife.hospital.repository;

import com.newlife.hospital.domain.Enums.Role;
import com.newlife.hospital.domain.StaffProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {

    Optional<StaffProfile> findByUserId(Long userId);

    @Query("select sp from StaffProfile sp join fetch sp.user u where u.role = :role order by sp.fullName")
    List<StaffProfile> findAllByRole(Role role);
}
