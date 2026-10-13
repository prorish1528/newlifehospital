package com.newlife.hospital.repository;

import com.newlife.hospital.domain.Invoice;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    @Override
    @EntityGraph(attributePaths = {"patient", "doctor"})
    Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"patient", "doctor", "items"})
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findDetailedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(Long id);

    @Query(value = "select nextval('invoice_number_seq')", nativeQuery = true)
    long nextInvoiceSequence();

    /** Rows of [count, billed (net), collected (paid), outstanding (balance)] for the date range. */
    @Query("""
            select count(i), coalesce(sum(i.netAmount), 0), coalesce(sum(i.paidAmount), 0),
                   coalesce(sum(i.balanceAmount), 0)
            from Invoice i where i.invoiceDate between :from and :to
            """)
    List<Object[]> totalsBetween(LocalDate from, LocalDate to);

    @Query("""
            select i.invoiceDate, count(i), coalesce(sum(i.netAmount), 0), coalesce(sum(i.paidAmount), 0)
            from Invoice i where i.invoiceDate between :from and :to
            group by i.invoiceDate order by i.invoiceDate
            """)
    List<Object[]> dailyBetween(LocalDate from, LocalDate to);

    @Query("""
            select year(i.invoiceDate), month(i.invoiceDate), count(i),
                   coalesce(sum(i.netAmount), 0), coalesce(sum(i.paidAmount), 0)
            from Invoice i where i.invoiceDate between :from and :to
            group by year(i.invoiceDate), month(i.invoiceDate)
            order by year(i.invoiceDate), month(i.invoiceDate)
            """)
    List<Object[]> monthlyBetween(LocalDate from, LocalDate to);

    @Query("""
            select year(i.invoiceDate), count(i), coalesce(sum(i.netAmount), 0), coalesce(sum(i.paidAmount), 0)
            from Invoice i group by year(i.invoiceDate) order by year(i.invoiceDate)
            """)
    List<Object[]> yearly();
}
