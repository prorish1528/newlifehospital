package com.newlife.hospital.repository;

import com.newlife.hospital.domain.Doctor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByActiveTrueOrderByNameAsc();

    List<Doctor> findAllByOrderByNameAsc();
}
