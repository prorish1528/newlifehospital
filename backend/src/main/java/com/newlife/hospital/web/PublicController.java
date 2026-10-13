package com.newlife.hospital.web;

import com.newlife.hospital.service.CatalogService;
import com.newlife.hospital.service.FileStorageService;
import com.newlife.hospital.web.dto.PublicDtos.DoctorView;
import com.newlife.hospital.web.dto.PublicDtos.ServiceView;
import com.newlife.hospital.web.dto.PublicDtos.ValidationRuleView;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated, read-only endpoints for the public website and form validation metadata. */
@RestController
public class PublicController {

    private final CatalogService catalog;
    private final FileStorageService files;

    public PublicController(CatalogService catalog, FileStorageService files) {
        this.catalog = catalog;
        this.files = files;
    }

    @GetMapping("/api/public/cms")
    public Map<String, String> cms() {
        return catalog.cmsMap();
    }

    @GetMapping("/api/public/doctors")
    public List<DoctorView> doctors() {
        return catalog.doctors(false);
    }

    @GetMapping("/api/public/services")
    public List<ServiceView> services() {
        return catalog.services(false);
    }

    @GetMapping("/api/public/files/doctors/{name}")
    public ResponseEntity<Resource> doctorPhoto(@PathVariable String name) {
        return files.loadDoctorPhoto(name)
                .map(f -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(f.contentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                        .header("X-Content-Type-Options", "nosniff")
                        .<Resource>body(new FileSystemResource(f.path())))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Validation rules for one or more forms, e.g. {@code /api/validations?forms=patient,invoice}. */
    @GetMapping("/api/validations")
    public List<ValidationRuleView> validations(@RequestParam String forms) {
        List<String> names = Arrays.stream(forms.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .limit(10).toList();
        return catalog.rules(names);
    }
}
