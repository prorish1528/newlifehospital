package com.newlife.hospital.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record CreateStaffRequest(String username, String password, String fullName, String email,
                                     String phoneNumber) {}

    public record UpdateStaffRequest(String fullName, String email, String phoneNumber, Boolean enabled) {}

    public record ResetPasswordRequest(String newPassword) {}

    public record ResetUsernameRequest(String newUsername) {}

    public record StaffView(Long id, String username, String fullName, String email, String phoneNumber,
                            boolean enabled, Instant createdAt) {}

    public record DoctorRequest(String name, String specialization, String qualification, Integer experienceYears,
                                Boolean active) {}

    public record ServiceRequest(String serviceName, String category, BigDecimal cost, String description,
                                 Boolean active) {}

    public record CmsUpdateRequest(String contentText) {}

    public record CmsView(String sectionKey, String label, String contentText, String updatedBy, Instant updatedAt) {}

    public record Totals(long invoiceCount, BigDecimal billed, BigDecimal collected, BigDecimal outstanding) {}

    public record RevenuePoint(String label, long invoiceCount, BigDecimal billed, BigDecimal collected) {}

    public record Dashboard(Totals today, Totals thisMonth, Totals thisYear, List<RevenuePoint> daily,
                            List<RevenuePoint> monthly, List<RevenuePoint> yearly) {}
}
