package com.newlife.hospital.repository;

import com.newlife.hospital.domain.FieldValidation;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldValidationRepository extends JpaRepository<FieldValidation, Long> {

    List<FieldValidation> findByFormNameOrderByIdAsc(String formName);

    List<FieldValidation> findByFormNameInOrderByIdAsc(Collection<String> formNames);
}
