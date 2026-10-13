package com.newlife.hospital.service;

import com.newlife.hospital.domain.CmsContent;
import com.newlife.hospital.domain.Doctor;
import com.newlife.hospital.domain.FieldValidation;
import com.newlife.hospital.domain.HospitalService;
import com.newlife.hospital.domain.Invoice;
import com.newlife.hospital.domain.Patient;
import com.newlife.hospital.domain.StaffProfile;
import com.newlife.hospital.web.dto.AdminDtos.CmsView;
import com.newlife.hospital.web.dto.AdminDtos.StaffView;
import com.newlife.hospital.web.dto.InvoiceDtos.InvoiceDetail;
import com.newlife.hospital.web.dto.InvoiceDtos.InvoiceSummary;
import com.newlife.hospital.web.dto.InvoiceDtos.ItemView;
import com.newlife.hospital.web.dto.PatientDtos.PatientDetail;
import com.newlife.hospital.web.dto.PatientDtos.PatientSummary;
import com.newlife.hospital.web.dto.PublicDtos.DoctorView;
import com.newlife.hospital.web.dto.PublicDtos.ServiceView;
import com.newlife.hospital.web.dto.PublicDtos.ValidationRuleView;
import java.time.LocalDate;
import java.time.Period;

/** Entity to DTO conversions. Entities never leave the service layer. */
public final class Mappers {

    private Mappers() {
    }

    public static int ageOn(LocalDate dob, LocalDate today) {
        return Math.max(0, Period.between(dob, today).getYears());
    }

    /** Government IDs are only ever exposed masked. */
    public static String maskId(String last4) {
        return "XXXX-" + last4;
    }

    public static PatientSummary summary(Patient p, LocalDate today) {
        return new PatientSummary(p.getId(), p.getPatientCode(), p.getName(), ageOn(p.getDob(), today),
                p.getGender().name(), p.getContactNumber(), p.getIdType().name(), maskId(p.getIdNumberLast4()));
    }

    public static PatientDetail detail(Patient p, LocalDate today) {
        return new PatientDetail(p.getId(), p.getPatientCode(), p.getName(), p.getIdType().name(), p.getIdTypeOther(),
                maskId(p.getIdNumberLast4()), p.getDob(), ageOn(p.getDob(), today), p.getGender().name(),
                p.getContactNumber(), p.getAddress(), p.getCreatedBy(), p.getCreatedAt(), p.getUpdatedAt());
    }

    public static DoctorView doctor(Doctor d) {
        return new DoctorView(d.getId(), d.getName(), d.getSpecialization(), d.getQualification(),
                d.getExperienceYears(), d.getPhotoUrl(), d.isActive());
    }

    public static ServiceView service(HospitalService s) {
        return new ServiceView(s.getId(), s.getServiceName(), s.getCategory(), s.getCost(), s.getDescription(),
                s.isActive());
    }

    public static ValidationRuleView rule(FieldValidation v) {
        return new ValidationRuleView(v.getFormName(), v.getFieldName(), v.getFieldLabel(), v.getDataType().name(),
                v.isRequired(), v.getRegexPattern(), v.getMinLength(), v.getMaxLength(), v.getErrorMessage());
    }

    public static CmsView cms(CmsContent c) {
        return new CmsView(c.getSectionKey(), c.getLabel(), c.getContentText(), c.getUpdatedBy(), c.getUpdatedAt());
    }

    public static StaffView staff(StaffProfile p) {
        return new StaffView(p.getUser().getId(), p.getUser().getUsername(), p.getFullName(), p.getEmail(),
                p.getPhoneNumber(), p.getUser().isEnabled(), p.getCreatedAt());
    }

    public static InvoiceSummary invoiceSummary(Invoice i) {
        return new InvoiceSummary(i.getId(), i.getInvoiceNumber(), i.getInvoiceDate(), i.getPatient().getPatientCode(),
                i.getPatient().getName(), i.getDoctor() == null ? null : i.getDoctor().getName(), i.getStaffName(),
                i.getNetAmount(), i.getPaidAmount(), i.getBalanceAmount(), i.getPaymentStatus().name(),
                i.getCreatedAt());
    }

    public static InvoiceDetail invoiceDetail(Invoice i, LocalDate today) {
        Doctor d = i.getDoctor();
        return new InvoiceDetail(i.getId(), i.getInvoiceNumber(), i.getInvoiceDate(), summary(i.getPatient(), today),
                d == null ? null : d.getId(), d == null ? null : d.getName(), d == null ? null : d.getSpecialization(),
                i.getStaffUsername(), i.getStaffName(),
                i.getItems().stream().map(it -> new ItemView(it.getService().getId(), it.getServiceName(),
                        it.getUnitPrice(), it.getQuantity(), it.getAmount())).toList(),
                i.getTotalAmount(), i.getDiscount(), i.getNetAmount(), i.getPaidAmount(), i.getBalanceAmount(),
                i.getPaymentStatus().name(), i.getCreatedAt(), i.getUpdatedAt());
    }
}
