package com.newlife.hospital.domain;

/** Namespace for the small enums shared across the domain model. */
public final class Enums {

    private Enums() {
    }

    public enum Role { ADMIN, STAFF }

    public enum IdType { AADHAAR, PASSPORT, VOTER_ID, DRIVING_LICENSE, OTHER }

    public enum Gender { MALE, FEMALE, OTHER }

    public enum PaymentStatus { PARTIAL, FULL }

    public enum DataType { STRING, NUMBER, DECIMAL, DATE, EMAIL, PHONE }
}
