package com.newlife.hospital.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private HospitalService service;

    @Column(name = "service_name", nullable = false, length = 120)
    private String serviceName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    protected InvoiceItem() {
    }

    /** Snapshots the service name and price so later charge-sheet edits never alter issued invoices. */
    public InvoiceItem(HospitalService service, int quantity) {
        this.service = service;
        this.serviceName = service.getServiceName();
        this.unitPrice = service.getCost();
        this.quantity = quantity;
        this.amount = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Long getId() { return id; }
    public HospitalService getService() { return service; }
    public String getServiceName() { return serviceName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getAmount() { return amount; }
}
