package com.newlife.hospital.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class InvoiceDtos {

    private InvoiceDtos() {
    }

    /** Prices are never accepted from the client; they are read from the charges master. */
    public record CreateInvoiceRequest(Long patientId, Long doctorId, List<ItemRequest> items, BigDecimal discount,
                                       String paymentStatus, BigDecimal paidAmount) {}

    public record ItemRequest(Long serviceId, Integer quantity) {}

    public record PaymentRequest(BigDecimal amount) {}

    public record ItemView(Long serviceId, String serviceName, BigDecimal unitPrice, int quantity, BigDecimal amount) {}

    public record InvoiceSummary(Long id, String invoiceNumber, LocalDate invoiceDate, String patientCode,
                                 String patientName, String doctorName, String staffName, BigDecimal netAmount,
                                 BigDecimal paidAmount, BigDecimal balanceAmount, String paymentStatus,
                                 Instant createdAt) {}

    public record InvoiceDetail(Long id, String invoiceNumber, LocalDate invoiceDate, PatientDtos.PatientSummary patient,
                                Long doctorId, String doctorName, String doctorSpecialization, String staffUsername,
                                String staffName, List<ItemView> items, BigDecimal totalAmount, BigDecimal discount,
                                BigDecimal netAmount, BigDecimal paidAmount, BigDecimal balanceAmount,
                                String paymentStatus, Instant createdAt, Instant updatedAt) {}
}
