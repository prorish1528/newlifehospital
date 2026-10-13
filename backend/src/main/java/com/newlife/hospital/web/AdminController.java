package com.newlife.hospital.web;

import com.newlife.hospital.security.AuthenticatedUser;
import com.newlife.hospital.service.CatalogService;
import com.newlife.hospital.service.DashboardService;
import com.newlife.hospital.service.InvoiceService;
import com.newlife.hospital.service.StaffService;
import com.newlife.hospital.web.dto.AdminDtos.CmsUpdateRequest;
import com.newlife.hospital.web.dto.AdminDtos.CmsView;
import com.newlife.hospital.web.dto.AdminDtos.CreateStaffRequest;
import com.newlife.hospital.web.dto.AdminDtos.Dashboard;
import com.newlife.hospital.web.dto.AdminDtos.DoctorRequest;
import com.newlife.hospital.web.dto.AdminDtos.ResetPasswordRequest;
import com.newlife.hospital.web.dto.AdminDtos.ResetUsernameRequest;
import com.newlife.hospital.web.dto.AdminDtos.ServiceRequest;
import com.newlife.hospital.web.dto.AdminDtos.StaffView;
import com.newlife.hospital.web.dto.AdminDtos.UpdateStaffRequest;
import com.newlife.hospital.web.dto.InvoiceDtos.InvoiceDetail;
import com.newlife.hospital.web.dto.InvoiceDtos.InvoiceSummary;
import com.newlife.hospital.web.dto.PublicDtos.DoctorView;
import com.newlife.hospital.web.dto.PublicDtos.PageResponse;
import com.newlife.hospital.web.dto.PublicDtos.ServiceView;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Admin portal: staff accounts, charges sheet, doctors, CMS, invoices and the revenue dashboard. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final StaffService staff;
    private final CatalogService catalog;
    private final InvoiceService invoices;
    private final DashboardService dashboard;

    public AdminController(StaffService staff, CatalogService catalog, InvoiceService invoices,
                           DashboardService dashboard) {
        this.staff = staff;
        this.catalog = catalog;
        this.invoices = invoices;
        this.dashboard = dashboard;
    }

    // ---- staff accounts -----------------------------------------------------------------------------------

    @GetMapping("/staff")
    public List<StaffView> listStaff() {
        return staff.list();
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffView createStaff(@RequestBody CreateStaffRequest request, @AuthenticationPrincipal AuthenticatedUser admin) {
        return staff.create(request, admin.username());
    }

    @PutMapping("/staff/{userId}")
    public StaffView updateStaff(@PathVariable Long userId, @RequestBody UpdateStaffRequest request,
                                 @AuthenticationPrincipal AuthenticatedUser admin) {
        return staff.update(userId, request, admin.username());
    }

    @DeleteMapping("/staff/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStaff(@PathVariable Long userId, @AuthenticationPrincipal AuthenticatedUser admin) {
        staff.delete(userId, admin.username());
    }

    @PostMapping("/staff/{userId}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long userId, @RequestBody ResetPasswordRequest request,
                              @AuthenticationPrincipal AuthenticatedUser admin) {
        staff.resetPassword(userId, request, admin.username());
    }

    @PostMapping("/staff/{userId}/reset-username")
    public StaffView resetUsername(@PathVariable Long userId, @RequestBody ResetUsernameRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser admin) {
        return staff.resetUsername(userId, request, admin.username());
    }

    // ---- charges sheet ------------------------------------------------------------------------------------

    @GetMapping("/services")
    public List<ServiceView> listServices() {
        return catalog.services(true);
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceView createService(@RequestBody ServiceRequest request) {
        return catalog.createService(request);
    }

    @PutMapping("/services/{id}")
    public ServiceView updateService(@PathVariable Long id, @RequestBody ServiceRequest request) {
        return catalog.updateService(id, request);
    }

    // ---- doctors ------------------------------------------------------------------------------------------

    @GetMapping("/doctors")
    public List<DoctorView> listDoctors() {
        return catalog.doctors(true);
    }

    @PostMapping("/doctors")
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorView createDoctor(@RequestBody DoctorRequest request) {
        return catalog.createDoctor(request);
    }

    @PutMapping("/doctors/{id}")
    public DoctorView updateDoctor(@PathVariable Long id, @RequestBody DoctorRequest request) {
        return catalog.updateDoctor(id, request);
    }

    @PostMapping(value = "/doctors/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DoctorView uploadDoctorPhoto(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return catalog.updateDoctorPhoto(id, file);
    }

    // ---- CMS ----------------------------------------------------------------------------------------------

    @GetMapping("/cms")
    public List<CmsView> listCms() {
        return catalog.cmsEntries();
    }

    @PutMapping("/cms/{key}")
    public CmsView updateCms(@PathVariable String key, @RequestBody CmsUpdateRequest request,
                             @AuthenticationPrincipal AuthenticatedUser admin) {
        return catalog.updateCms(key, request, admin.username());
    }

    // ---- invoices & dashboard -----------------------------------------------------------------------------

    @GetMapping("/invoices")
    public PageResponse<InvoiceSummary> listInvoices(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invoices.search(query, from, to, status, null, page, size);
    }

    @GetMapping("/invoices/{id}")
    public InvoiceDetail getInvoice(@PathVariable Long id) {
        return invoices.get(id);
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard() {
        return dashboard.dashboard();
    }
}
