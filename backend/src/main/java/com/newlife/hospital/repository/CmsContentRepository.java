package com.newlife.hospital.repository;

import com.newlife.hospital.domain.CmsContent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CmsContentRepository extends JpaRepository<CmsContent, Long> {

    Optional<CmsContent> findBySectionKey(String sectionKey);

    List<CmsContent> findAllByOrderByIdAsc();
}
