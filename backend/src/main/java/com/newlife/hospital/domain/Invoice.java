package com.newlife.hospital.domain;

import com.newlife.hospital.domain.Enums.PaymentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, updatable = false, length = 30)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    @Column(name = "staff_username", nullable = false, updatable = false, length = 30)
    private String staffUsername;

    @Column(name = "staff_name", nullable = false, updatable = false, length = 100)
    private String staffName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", updatable = false)
    private Doctor doctor;

    @Column(name = "total_amount", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(name = "net_amount", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "balance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 10)
    private PaymentStatus paymentStatus;

    @Column(name = "invoice_date", nullable = false, updatable = false)
    private LocalDate invoiceDate;

    /** When the patient visited; defaults to the billing time but staff may enter an earlier time. */
    @Column(name = "visit_at", nullable = false, updatable = false)
    private Instant visitAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<InvoiceItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Invoice() {
    }

    public Invoice(String invoiceNumber, Patient patient, String staffUsername, String staffName, Doctor doctor,
                   BigDecimal discount, LocalDate invoiceDate, Instant visitAt) {
        this.visitAt = visitAt;
        this.invoiceNumber = invoiceNumber;
        this.patient = patient;
        this.staffUsername = staffUsername;
        this.staffName = staffName;
        this.doctor = doctor;
        this.discount = discount;
        this.invoiceDate = invoiceDate;
    }

    public void addItem(InvoiceItem item) {
        item.setInvoice(this);
        items.add(item);
    }

    /** Sets totals from the line items; paid amount must already respect 0 <= paid <= net. */
    public void applyTotals(BigDecimal total, BigDecimal paid) {
        this.totalAmount = total;
        this.netAmount = total.subtract(discount);
        applyPaid(paid);
    }

    public void applyPaid(BigDecimal paid) {
        this.paidAmount = paid;
        this.balanceAmount = netAmount.subtract(paid);
        this.paymentStatus = balanceAmount.signum() == 0 ? PaymentStatus.FULL : PaymentStatus.PARTIAL;
    }

    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public Patient getPatient() { return patient; }
    public String getStaffUsername() { return staffUsername; }
    public String getStaffName() { return staffName; }
    public Doctor getDoctor() { return doctor; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public Instant getVisitAt() { return visitAt; }
    public List<InvoiceItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
