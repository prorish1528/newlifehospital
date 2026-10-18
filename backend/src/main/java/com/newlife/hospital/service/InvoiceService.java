package com.newlife.hospital.service;

import com.newlife.hospital.config.AppProperties;
import com.newlife.hospital.domain.Doctor;
import com.newlife.hospital.domain.Enums.PaymentStatus;
import com.newlife.hospital.domain.HospitalService;
import com.newlife.hospital.domain.Invoice;
import com.newlife.hospital.domain.InvoiceItem;
import com.newlife.hospital.domain.Patient;
import com.newlife.hospital.exception.ApiException;
import com.newlife.hospital.repository.DoctorRepository;
import com.newlife.hospital.repository.HospitalServiceRepository;
import com.newlife.hospital.repository.InvoiceRepository;
import com.newlife.hospital.repository.PatientRepository;
import com.newlife.hospital.security.AuthenticatedUser;
import com.newlife.hospital.validation.DynamicValidator;
import com.newlife.hospital.web.dto.InvoiceDtos.CreateInvoiceRequest;
import com.newlife.hospital.web.dto.InvoiceDtos.InvoiceDetail;
import com.newlife.hospital.web.dto.InvoiceDtos.InvoiceSummary;
import com.newlife.hospital.web.dto.InvoiceDtos.ItemRequest;
import com.newlife.hospital.web.dto.InvoiceDtos.PaymentRequest;
import com.newlife.hospital.web.dto.PublicDtos.PageResponse;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final int MAX_LINE_ITEMS = 50;
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InvoiceRepository invoices;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final HospitalServiceRepository services;
    private final DynamicValidator validator;
    private final AuthService authService;
    private final AppProperties props;

    public InvoiceService(InvoiceRepository invoices, PatientRepository patients, DoctorRepository doctors,
                          HospitalServiceRepository services, DynamicValidator validator, AuthService authService,
                          AppProperties props) {
        this.invoices = invoices;
        this.patients = patients;
        this.doctors = doctors;
        this.services = services;
        this.validator = validator;
        this.authService = authService;
        this.props = props;
    }

    @Transactional
    public InvoiceDetail create(CreateInvoiceRequest req, AuthenticatedUser staff) {
        var check = validator.form("invoice");
        BigDecimal discount = req.discount() == null ? BigDecimal.ZERO : req.discount();
        check.check("discount", discount.toPlainString());

        PaymentStatus status = null;
        try {
            status = PaymentStatus.valueOf(String.valueOf(req.paymentStatus()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            check.reject("paymentStatus", "Select Partially Paid or Fully Paid.");
        }
        if (status == PaymentStatus.PARTIAL) {
            check.check("paidAmount", req.paidAmount() == null ? null : req.paidAmount().toPlainString());
        }

        Instant visitAt = parseVisitTime(req.visitAt(), check);

        Patient patient = req.patientId() == null ? null : patients.findById(req.patientId()).orElse(null);
        if (patient == null) {
            check.reject("patientId", "Select a registered patient.");
        }
        Doctor doctor = null;
        if (req.doctorId() != null) {
            doctor = doctors.findById(req.doctorId()).filter(Doctor::isActive).orElse(null);
            if (doctor == null) {
                check.reject("doctorId", "Selected doctor is not available.");
            }
        }

        List<ItemRequest> items = req.items() == null ? List.of() : req.items();
        if (items.isEmpty()) {
            check.reject("items", "Add at least one service.");
        } else if (items.size() > MAX_LINE_ITEMS) {
            check.reject("items", "An invoice can have at most " + MAX_LINE_ITEMS + " line items.");
        }
        var serviceIds = new HashSet<Long>();
        for (int i = 0; i < items.size(); i++) {
            ItemRequest item = items.get(i);
            check.check("items[" + i + "].quantity", "quantity", item.quantity());
            if (item.serviceId() == null || !serviceIds.add(item.serviceId())) {
                check.reject("items[" + i + "].serviceId", "Select each service once; adjust the quantity instead.");
            }
        }
        Map<Long, HospitalService> catalog = services.findAllById(serviceIds).stream()
                .filter(HospitalService::isActive)
                .collect(Collectors.toMap(HospitalService::getId, Function.identity()));
        for (int i = 0; i < items.size(); i++) {
            Long id = items.get(i).serviceId();
            if (id != null && !catalog.containsKey(id)) {
                check.reject("items[" + i + "].serviceId", "This service is not available.");
            }
        }
        check.throwIfInvalid();

        var lines = new ArrayList<InvoiceItem>();
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : items) {
            InvoiceItem line = new InvoiceItem(catalog.get(item.serviceId()), item.quantity());
            lines.add(line);
            total = total.add(line.getAmount());
        }
        total = money(total);
        discount = money(discount);
        if (discount.compareTo(total) > 0) {
            check.reject("discount", "Discount cannot exceed the total amount.").throwIfInvalid();
        }
        BigDecimal net = total.subtract(discount);
        BigDecimal paid;
        if (status == PaymentStatus.FULL || net.signum() == 0) {
            paid = net;
        } else {
            paid = money(req.paidAmount());
            if (paid.compareTo(net) >= 0) {
                check.reject("paidAmount", "For a partial payment the amount paid must be less than "
                        + net.toPlainString() + ". Choose Fully Paid instead.").throwIfInvalid();
            }
        }

        LocalDate today = LocalDate.now(props.zone());
        String number = "NLH-INV-%s-%05d".formatted(today.format(NUMBER_DATE), invoices.nextInvoiceSequence());
        Invoice invoice = new Invoice(number, patient, staff.username(), authService.displayName(staff), doctor,
                discount, today, visitAt);
        lines.forEach(invoice::addItem);
        invoice.applyTotals(total, paid);
        return Mappers.invoiceDetail(invoices.saveAndFlush(invoice), today);
    }

    /** Records an additional payment against an outstanding balance. Row-locked to prevent double counting. */
    @Transactional
    public InvoiceDetail recordPayment(Long id, PaymentRequest req) {
        Invoice invoice = invoices.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Invoice"));
        var check = validator.form("invoice")
                .check("amount", "paidAmount", req.amount() == null ? null : req.amount().toPlainString());
        check.throwIfInvalid();
        BigDecimal amount = money(req.amount());
        if (amount.signum() <= 0) {
            check.reject("amount", "Payment amount must be greater than zero.").throwIfInvalid();
        }
        if (amount.compareTo(invoice.getBalanceAmount()) > 0) {
            check.reject("amount", "Payment cannot exceed the balance of "
                    + invoice.getBalanceAmount().toPlainString() + ".").throwIfInvalid();
        }
        invoice.applyPaid(invoice.getPaidAmount().add(amount));
        invoices.flush();
        return get(id);
    }

    @Transactional(readOnly = true)
    public InvoiceDetail get(Long id) {
        return invoices.findDetailedById(id)
                .map(i -> Mappers.invoiceDetail(i, LocalDate.now(props.zone())))
                .orElseThrow(() -> ApiException.notFound("Invoice"));
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummary> search(String query, LocalDate from, LocalDate to, String status,
                                               Long patientId, int page, int size) {
        Specification<Invoice> spec = (root, cq, cb) -> cb.conjunction();
        if (from != null) {
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("invoiceDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("invoiceDate"), to));
        }
        if (status != null && !status.isBlank()) {
            PaymentStatus ps;
            try {
                ps = PaymentStatus.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("Unknown payment status.");
            }
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("paymentStatus"), ps));
        }
        if (patientId != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        String q = Text.clean(query);
        if (q != null) {
            String pattern = Text.likeContains(q);
            spec = spec.and((root, cq, cb) -> {
                var patient = root.join("patient", JoinType.INNER);
                return cb.or(
                        cb.like(cb.lower(root.get("invoiceNumber")), pattern, '\\'),
                        cb.like(cb.lower(patient.get("name")), pattern, '\\'),
                        cb.equal(patient.get("patientCode"), q.toUpperCase(Locale.ROOT)));
            });
        }
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(invoices.findAll(spec, pageable).map(Mappers::invoiceSummary));
    }

    /**
     * Visit time as an ISO instant with offset (e.g. {@code 2026-10-18T10:08:00Z}, as sent by the SPA), or a local
     * {@code yyyy-MM-ddTHH:mm} taken as hospital time (IST). Blank means "now"; it may not be in the future
     * (5 minutes of clock skew allowed) or more than a year ago.
     */
    private Instant parseVisitTime(String raw, DynamicValidator.FormCheck check) {
        Instant now = Instant.now();
        if (raw == null || raw.isBlank()) {
            return now;
        }
        try {
            String value = raw.trim();
            Instant visit = hasOffset(value) ? OffsetDateTime.parse(value).toInstant()
                    : LocalDateTime.parse(value).atZone(props.zone()).toInstant();
            if (visit.isAfter(now.plus(Duration.ofMinutes(5)))) {
                check.reject("visitAt", "Visit time cannot be in the future.");
            } else if (visit.isBefore(now.minus(Duration.ofDays(365)))) {
                check.reject("visitAt", "Visit time cannot be more than a year ago.");
            }
            return visit;
        } catch (DateTimeParseException e) {
            check.reject("visitAt", "Enter a valid visit date and time.");
            return now;
        }
    }

    private static boolean hasOffset(String value) {
        return value.endsWith("Z") || value.matches(".*T.*[+-]\\d{2}:\\d{2}$");
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
