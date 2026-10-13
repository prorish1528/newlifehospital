package com.newlife.hospital.web;

import com.newlife.hospital.security.AuthenticatedUser;
import com.newlife.hospital.service.InvoiceService;
import com.newlife.hospital.service.PatientService;
import com.newlife.hospital.web.dto.InvoiceDtos.CreateInvoiceRequest;
import com.newlife.hospital.web.dto.InvoiceDtos.InvoiceDetail;
import com.newlife.hospital.web.dto.InvoiceDtos.InvoiceSummary;
import com.newlife.hospital.web.dto.InvoiceDtos.PaymentRequest;
import com.newlife.hospital.web.dto.PatientDtos.CreatePatientRequest;
import com.newlife.hospital.web.dto.PatientDtos.IdCheckRequest;
import com.newlife.hospital.web.dto.PatientDtos.IdCheckResponse;
import com.newlife.hospital.web.dto.PatientDtos.PatientDetail;
import com.newlife.hospital.web.dto.PatientDtos.PatientSummary;
import com.newlife.hospital.web.dto.PatientDtos.UpdatePatientRequest;
import com.newlife.hospital.web.dto.PublicDtos.PageResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Staff portal: patient registration, lookup and billing. */
@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasRole('STAFF')")
public class StaffController {

    private final PatientService patients;
    private final InvoiceService invoices;

    public StaffController(PatientService patients, InvoiceService invoices) {
        this.patients = patients;
        this.invoices = invoices;
    }

    // ---- patients -----------------------------------------------------------------------------------------

    @PostMapping("/patients")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDetail register(@RequestBody CreatePatientRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return patients.register(request, user.username());
    }

    @GetMapping("/patients/suggest")
    public List<PatientSummary> suggest(@RequestParam("q") String query) {
        return patients.suggest(query);
    }

    /** POST so the government ID number travels in the body, never in a URL or access log. */
    @PostMapping("/patients/check-id")
    public IdCheckResponse checkId(@RequestBody IdCheckRequest request) {
        return patients.checkId(request);
    }

    @GetMapping("/patients")
    public PageResponse<PatientSummary> search(@RequestParam(name = "q", required = false) String query,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return patients.search(query, page, size);
    }

    @GetMapping("/patients/{id}")
    public PatientDetail get(@PathVariable Long id) {
        return patients.get(id);
    }

    @PatchMapping("/patients/{id}")
    public PatientDetail updateContact(@PathVariable Long id, @RequestBody UpdatePatientRequest request) {
        return patients.updateContact(id, request);
    }

    // ---- invoices -----------------------------------------------------------------------------------------

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDetail createInvoice(@RequestBody CreateInvoiceRequest request,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return invoices.create(request, user);
    }

    @GetMapping("/invoices")
    public PageResponse<InvoiceSummary> listInvoices(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invoices.search(query, from, to, status, patientId, page, size);
    }

    @GetMapping("/invoices/{id}")
    public InvoiceDetail getInvoice(@PathVariable Long id) {
        return invoices.get(id);
    }

    @PostMapping("/invoices/{id}/payments")
    public InvoiceDetail recordPayment(@PathVariable Long id, @RequestBody PaymentRequest request) {
        return invoices.recordPayment(id, request);
    }
}
