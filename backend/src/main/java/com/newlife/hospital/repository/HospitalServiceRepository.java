package com.newlife.hospital.repository;

import com.newlife.hospital.domain.HospitalService;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalServiceRepository extends JpaRepository<HospitalService, Long> {

    List<HospitalService> findByActiveTrueOrderByCategoryAscServiceNameAsc();

    List<HospitalService> findAllByOrderByCategoryAscServiceNameAsc();

    boolean existsByServiceNameIgnoreCase(String serviceName);

    boolean existsByServiceNameIgnoreCaseAndIdNot(String serviceName, Long id);
}
