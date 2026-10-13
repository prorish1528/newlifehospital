package com.newlife.hospital.service;

import com.newlife.hospital.domain.CmsContent;
import com.newlife.hospital.domain.Doctor;
import com.newlife.hospital.domain.HospitalService;
import com.newlife.hospital.exception.ApiException;
import com.newlife.hospital.repository.CmsContentRepository;
import com.newlife.hospital.repository.DoctorRepository;
import com.newlife.hospital.repository.FieldValidationRepository;
import com.newlife.hospital.repository.HospitalServiceRepository;
import com.newlife.hospital.validation.DynamicValidator;
import com.newlife.hospital.web.dto.AdminDtos.CmsUpdateRequest;
import com.newlife.hospital.web.dto.AdminDtos.CmsView;
import com.newlife.hospital.web.dto.AdminDtos.DoctorRequest;
import com.newlife.hospital.web.dto.AdminDtos.ServiceRequest;
import com.newlife.hospital.web.dto.PublicDtos.DoctorView;
import com.newlife.hospital.web.dto.PublicDtos.ServiceView;
import com.newlife.hospital.web.dto.PublicDtos.ValidationRuleView;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Doctors, the charges sheet, CMS text and validation metadata: the hospital's reference data. */
@Service
public class CatalogService {

    private final DoctorRepository doctors;
    private final HospitalServiceRepository services;
    private final CmsContentRepository cms;
    private final FieldValidationRepository validations;
    private final DynamicValidator validator;
    private final FileStorageService files;

    public CatalogService(DoctorRepository doctors, HospitalServiceRepository services, CmsContentRepository cms,
                          FieldValidationRepository validations, DynamicValidator validator, FileStorageService files) {
        this.doctors = doctors;
        this.services = services;
        this.cms = cms;
        this.validations = validations;
        this.validator = validator;
        this.files = files;
    }

    // ---- validation metadata ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ValidationRuleView> rules(Collection<String> forms) {
        return validations.findByFormNameInOrderByIdAsc(forms).stream().map(Mappers::rule).toList();
    }

    // ---- doctors ------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DoctorView> doctors(boolean includeInactive) {
        var list = includeInactive ? doctors.findAllByOrderByNameAsc() : doctors.findByActiveTrueOrderByNameAsc();
        return list.stream().map(Mappers::doctor).toList();
    }

    @Transactional
    public DoctorView createDoctor(DoctorRequest req) {
        Doctor doctor = new Doctor();
        applyDoctor(doctor, req);
        return Mappers.doctor(doctors.save(doctor));
    }

    @Transactional
    public DoctorView updateDoctor(Long id, DoctorRequest req) {
        Doctor doctor = findDoctor(id);
        applyDoctor(doctor, req);
        return Mappers.doctor(doctor);
    }

    @Transactional
    public DoctorView updateDoctorPhoto(Long id, MultipartFile file) {
        Doctor doctor = findDoctor(id);
        String old = doctor.getPhotoUrl();
        doctor.setPhotoUrl(files.storeDoctorPhoto(file));
        files.deleteDoctorPhoto(old);
        return Mappers.doctor(doctor);
    }

    private void applyDoctor(Doctor doctor, DoctorRequest req) {
        String name = Text.clean(req.name());
        String specialization = Text.clean(req.specialization());
        String qualification = Text.clean(req.qualification());
        var check = validator.form("doctor")
                .check("name", name)
                .check("specialization", specialization)
                .check("qualification", qualification)
                .check("experienceYears", req.experienceYears());
        if (req.experienceYears() != null && (req.experienceYears() < 0 || req.experienceYears() > 70)) {
            check.reject("experienceYears", "Experience must be between 0 and 70 years.");
        }
        check.throwIfInvalid();
        doctor.setName(name);
        doctor.setSpecialization(specialization);
        doctor.setQualification(qualification);
        doctor.setExperienceYears(req.experienceYears());
        if (req.active() != null) {
            doctor.setActive(req.active());
        }
    }

    private Doctor findDoctor(Long id) {
        return doctors.findById(id).orElseThrow(() -> ApiException.notFound("Doctor"));
    }

    // ---- charges sheet ------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ServiceView> services(boolean includeInactive) {
        var list = includeInactive ? services.findAllByOrderByCategoryAscServiceNameAsc()
                : services.findByActiveTrueOrderByCategoryAscServiceNameAsc();
        return list.stream().map(Mappers::service).toList();
    }

    @Transactional
    public ServiceView createService(ServiceRequest req) {
        HospitalService service = new HospitalService();
        applyService(service, req, null);
        return Mappers.service(services.save(service));
    }

    @Transactional
    public ServiceView updateService(Long id, ServiceRequest req) {
        HospitalService service = services.findById(id).orElseThrow(() -> ApiException.notFound("Service"));
        applyService(service, req, id);
        return Mappers.service(service);
    }

    private void applyService(HospitalService service, ServiceRequest req, Long id) {
        String name = Text.clean(req.serviceName());
        String category = Text.clean(req.category());
        String description = Text.clean(req.description());
        var check = validator.form("service")
                .check("serviceName", name)
                .check("category", category)
                .check("cost", req.cost() == null ? null : req.cost().toPlainString())
                .check("description", description);
        if (!check.hasError("serviceName")) {
            boolean duplicate = id == null ? services.existsByServiceNameIgnoreCase(name)
                    : services.existsByServiceNameIgnoreCaseAndIdNot(name, id);
            if (duplicate) {
                check.reject("serviceName", "A service with this name already exists.");
            }
        }
        check.throwIfInvalid();
        service.setServiceName(name);
        service.setCategory(category);
        service.setCost(req.cost().setScale(2, RoundingMode.HALF_UP));
        service.setDescription(description);
        if (req.active() != null) {
            service.setActive(req.active());
        }
    }

    // ---- CMS ----------------------------------------------------------------------------------------------

    /** Public map of section key to text. */
    @Transactional(readOnly = true)
    public Map<String, String> cmsMap() {
        Map<String, String> map = new LinkedHashMap<>();
        cms.findAllByOrderByIdAsc().forEach(c -> map.put(c.getSectionKey(), c.getContentText()));
        return map;
    }

    @Transactional(readOnly = true)
    public List<CmsView> cmsEntries() {
        return cms.findAllByOrderByIdAsc().stream().map(Mappers::cms).toList();
    }

    @Transactional
    public CmsView updateCms(String key, CmsUpdateRequest req, String adminUsername) {
        CmsContent content = cms.findBySectionKey(key).orElseThrow(() -> ApiException.notFound("Content section"));
        String text = req.contentText() == null ? null : req.contentText().trim();
        validator.form("cms").check("contentText", text).throwIfInvalid();
        content.setContentText(text);
        content.setUpdatedBy(adminUsername);
        cms.flush();
        return Mappers.cms(content);
    }
}
