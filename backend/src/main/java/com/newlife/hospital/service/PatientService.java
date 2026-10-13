package com.newlife.hospital.service;

import com.newlife.hospital.config.AppProperties;
import com.newlife.hospital.domain.Enums.Gender;
import com.newlife.hospital.domain.Enums.IdType;
import com.newlife.hospital.domain.Patient;
import com.newlife.hospital.exception.ApiException;
import com.newlife.hospital.repository.PatientRepository;
import com.newlife.hospital.security.BlindIndexer;
import com.newlife.hospital.security.EncryptionUtil;
import com.newlife.hospital.validation.DynamicValidator;
import com.newlife.hospital.web.dto.PatientDtos.CreatePatientRequest;
import com.newlife.hospital.web.dto.PatientDtos.IdCheckRequest;
import com.newlife.hospital.web.dto.PatientDtos.IdCheckResponse;
import com.newlife.hospital.web.dto.PatientDtos.PatientDetail;
import com.newlife.hospital.web.dto.PatientDtos.PatientSummary;
import com.newlife.hospital.web.dto.PatientDtos.UpdatePatientRequest;
import com.newlife.hospital.web.dto.PublicDtos.PageResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

    /** Associated data that binds ID ciphertexts to their column. */
    static final String ID_NUMBER_AAD = "patients.id_number";
    private static final int MAX_AGE_YEARS = 130;

    private final PatientRepository patients;
    private final DynamicValidator validator;
    private final EncryptionUtil encryption;
    private final BlindIndexer blindIndexer;
    private final AppProperties props;

    public PatientService(PatientRepository patients, DynamicValidator validator, EncryptionUtil encryption,
                          BlindIndexer blindIndexer, AppProperties props) {
        this.patients = patients;
        this.validator = validator;
        this.encryption = encryption;
        this.blindIndexer = blindIndexer;
        this.props = props;
    }

    @Transactional
    public PatientDetail register(CreatePatientRequest req, String staffUsername) {
        String name = Text.clean(req.name());
        String idTypeRaw = Text.upper(req.idType());
        String other = Text.clean(req.idTypeOther());
        String idNumber = Text.idNumber(req.idNumber());
        String genderRaw = Text.upper(req.gender());
        String contact = Text.clean(req.contactNumber());
        String address = Text.clean(req.address());

        var check = validator.form("patient")
                .check("name", name)
                .check("idType", idTypeRaw)
                .check("dob", req.dob())
                .check("gender", genderRaw)
                .check("contactNumber", contact)
                .check("address", address);

        IdType idType = check.hasError("idType") ? null : IdType.valueOf(idTypeRaw);
        if (idType == IdType.OTHER) {
            check.check("idTypeOther", other);
            if (other == null) {
                check.reject("idTypeOther", "Please specify the ID type.");
            }
        } else {
            other = null;
        }
        String idRule = idType != null && check.hasRule("idNumber_" + idType.name()) ? "idNumber_" + idType.name() : "idNumber";
        check.check("idNumber", idRule, idNumber);

        LocalDate today = today();
        LocalDate dob = null;
        if (!check.hasError("dob")) {
            dob = LocalDate.parse(req.dob());
            if (dob.isAfter(today) || dob.isBefore(today.minusYears(MAX_AGE_YEARS))) {
                check.reject("dob", "Date of birth cannot be in the future or more than 130 years ago.");
            }
        }
        check.throwIfInvalid();

        String hash = idHash(idType, other, idNumber);
        patients.findByIdNumberHash(hash).ifPresent(existing -> {
            throw ApiException.conflict("A patient with this ID is already registered as "
                    + existing.getPatientCode() + " (" + existing.getName() + ").");
        });

        String code = "NLH-P%06d".formatted(patients.nextPatientSequence());
        Patient patient = new Patient(code, name, idType, other, encryption.encrypt(idNumber, ID_NUMBER_AAD), hash,
                idNumber.substring(Math.max(0, idNumber.length() - 4)), dob, Gender.valueOf(genderRaw), contact,
                address, staffUsername);
        return Mappers.detail(patients.saveAndFlush(patient), today);
    }

    /** Exact duplicate check on the blind index; the ID number never appears in a URL or a log. */
    @Transactional(readOnly = true)
    public IdCheckResponse checkId(IdCheckRequest req) {
        IdType idType;
        try {
            idType = IdType.valueOf(Text.upper(req.idType()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return new IdCheckResponse(false, null);
        }
        String number = Text.idNumber(req.idNumber());
        if (number == null) {
            return new IdCheckResponse(false, null);
        }
        String other = idType == IdType.OTHER ? Text.clean(req.idTypeOther()) : null;
        return patients.findByIdNumberHash(idHash(idType, other, number))
                .map(p -> new IdCheckResponse(true, Mappers.summary(p, today())))
                .orElse(new IdCheckResponse(false, null));
    }

    /** Auto-suggest by name fragment, exact patient code / contact number, or an exact standard ID number. */
    @Transactional(readOnly = true)
    public List<PatientSummary> suggest(String query) {
        String q = Text.clean(query);
        if (q == null || q.length() < 2) {
            return List.of();
        }
        return search(q, 0, 8).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientSummary> search(String query, int page, int size) {
        String q = Text.clean(query);
        LocalDate today = today();
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        if (q == null) {
            return PageResponse.of(patients.findAll(pageable.withSort(Sort.by(Sort.Direction.DESC, "createdAt")))
                    .map(p -> Mappers.summary(p, today)));
        }
        String idCandidate = Text.idNumber(q);
        List<String> idHashes = Arrays.stream(IdType.values())
                .filter(t -> t != IdType.OTHER)
                .map(t -> idHash(t, null, idCandidate))
                .toList();
        return PageResponse.of(patients.search(Text.likeContains(q), Text.upper(q), idHashes, pageable)
                .map(p -> Mappers.summary(p, today)));
    }

    @Transactional(readOnly = true)
    public PatientDetail get(Long id) {
        return Mappers.detail(find(id), today());
    }

    /** Only contact number and address are editable; identity fields are immutable by design. */
    @Transactional
    public PatientDetail updateContact(Long id, UpdatePatientRequest req) {
        Patient patient = find(id);
        String contact = Text.clean(req.contactNumber());
        String address = Text.clean(req.address());
        validator.form("patient")
                .check("contactNumber", contact)
                .check("address", address)
                .throwIfInvalid();
        patient.setContactNumber(contact);
        patient.setAddress(address);
        return Mappers.detail(patients.saveAndFlush(patient), today());
    }

    Optional<Patient> findOptional(Long id) {
        return id == null ? Optional.empty() : patients.findById(id);
    }

    private Patient find(Long id) {
        return findOptional(id).orElseThrow(() -> ApiException.notFound("Patient"));
    }

    private String idHash(IdType type, String other, String number) {
        String otherKey = other == null ? "" : other.toUpperCase();
        return blindIndexer.index(type.name() + "|" + otherKey + "|" + number);
    }

    private LocalDate today() {
        return LocalDate.now(props.zone());
    }
}
