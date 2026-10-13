package com.newlife.hospital.domain;

import com.newlife.hospital.domain.Enums.Gender;
import com.newlife.hospital.domain.Enums.IdType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A registered patient. Identity fields are immutable after registration (no setters); only contact number and
 * address may change, which enforces the "edit contact and address only" rule at the model level.
 */
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_code", nullable = false, unique = true, updatable = false, length = 20)
    private String patientCode;

    @Column(nullable = false, updatable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", nullable = false, updatable = false, length = 20)
    private IdType idType;

    @Column(name = "id_type_other", updatable = false, length = 50)
    private String idTypeOther;

    /** AES-256-GCM ciphertext of the normalized ID number. */
    @Column(name = "id_number", nullable = false, updatable = false, length = 512)
    private String idNumberEncrypted;

    /** HMAC-SHA256 blind index used for uniqueness and exact lookup. */
    @Column(name = "id_number_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String idNumberHash;

    @Column(name = "id_number_last4", nullable = false, updatable = false, length = 4)
    private String idNumberLast4;

    @Column(nullable = false, updatable = false)
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private Gender gender;

    @Column(name = "contact_number", nullable = false, length = 15)
    private String contactNumber;

    @Column(length = 255)
    private String address;

    @Column(name = "created_by", nullable = false, updatable = false, length = 30)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Patient() {
    }

    public Patient(String patientCode, String name, IdType idType, String idTypeOther, String idNumberEncrypted,
                   String idNumberHash, String idNumberLast4, LocalDate dob, Gender gender, String contactNumber,
                   String address, String createdBy) {
        this.patientCode = patientCode;
        this.name = name;
        this.idType = idType;
        this.idTypeOther = idTypeOther;
        this.idNumberEncrypted = idNumberEncrypted;
        this.idNumberHash = idNumberHash;
        this.idNumberLast4 = idNumberLast4;
        this.dob = dob;
        this.gender = gender;
        this.contactNumber = contactNumber;
        this.address = address;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public String getPatientCode() { return patientCode; }
    public String getName() { return name; }
    public IdType getIdType() { return idType; }
    public String getIdTypeOther() { return idTypeOther; }
    public String getIdNumberEncrypted() { return idNumberEncrypted; }
    public String getIdNumberLast4() { return idNumberLast4; }
    public LocalDate getDob() { return dob; }
    public Gender getGender() { return gender; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
