package com.newlife.hospital.web.dto;

import java.time.Instant;
import java.time.LocalDate;

public final class PatientDtos {

    private PatientDtos() {
    }

    /** Raw strings so every field is checked against the DB rules and reported with its configured message. */
    public record CreatePatientRequest(String name, String idType, String idTypeOther, String idNumber, String dob,
                                       String gender, String contactNumber, String address) {}

    /** Only these two fields are editable after registration; any other property is rejected as unknown. */
    public record UpdatePatientRequest(String contactNumber, String address) {}

    public record IdCheckRequest(String idType, String idTypeOther, String idNumber) {}

    public record IdCheckResponse(boolean exists, PatientSummary patient) {}

    public record PatientSummary(Long id, String patientCode, String name, int age, String gender,
                                 String contactNumber, String idType, String maskedIdNumber) {}

    public record PatientDetail(Long id, String patientCode, String name, String idType, String idTypeOther,
                                String maskedIdNumber, LocalDate dob, int age, String gender, String contactNumber,
                                String address, String createdBy, Instant createdAt, Instant updatedAt) {}
}
