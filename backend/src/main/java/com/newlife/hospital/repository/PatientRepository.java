package com.newlife.hospital.repository;

import com.newlife.hospital.domain.Patient;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByIdNumberHash(String idNumberHash);

    /** {@code pattern} must already be LIKE-escaped with backslash and wrapped in wildcards by the caller. */
    @Query("""
            select p from Patient p
            where lower(p.name) like :pattern escape '\\'
               or p.patientCode = :exact
               or p.contactNumber = :exact
               or p.idNumberHash in :idHashes
            order by p.name
            """)
    Page<Patient> search(String pattern, String exact, Collection<String> idHashes, Pageable pageable);

    @Query(value = "select nextval('patient_code_seq')", nativeQuery = true)
    long nextPatientSequence();
}
