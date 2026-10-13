package com.newlife.hospital.web.dto;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;

public final class PublicDtos {

    private PublicDtos() {
    }

    public record DoctorView(Long id, String name, String specialization, String qualification,
                             Integer experienceYears, String photoUrl, boolean active) {}

    public record ServiceView(Long id, String serviceName, String category, BigDecimal cost, String description,
                              boolean active) {}

    public record ValidationRuleView(String formName, String fieldName, String fieldLabel, String dataType,
                                     boolean required, String regexPattern, Integer minLength, Integer maxLength,
                                     String errorMessage) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                    page.getTotalPages());
        }
    }
}
