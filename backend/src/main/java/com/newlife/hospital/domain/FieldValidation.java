package com.newlife.hospital.domain;

import com.newlife.hospital.domain.Enums.DataType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "field_validations")
public class FieldValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_name", nullable = false, length = 50)
    private String formName;

    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    @Column(name = "field_label", nullable = false, length = 80)
    private String fieldLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private DataType dataType;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "regex_pattern", length = 500)
    private String regexPattern;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "error_message", nullable = false, length = 255)
    private String errorMessage;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FieldValidation() {
    }

    public FieldValidation(String formName, String fieldName, String fieldLabel, DataType dataType, boolean required,
                           String regexPattern, Integer minLength, Integer maxLength, String errorMessage) {
        this.formName = formName;
        this.fieldName = fieldName;
        this.fieldLabel = fieldLabel;
        this.dataType = dataType;
        this.required = required;
        this.regexPattern = regexPattern;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public String getFormName() { return formName; }
    public String getFieldName() { return fieldName; }
    public String getFieldLabel() { return fieldLabel; }
    public DataType getDataType() { return dataType; }
    public boolean isRequired() { return required; }
    public String getRegexPattern() { return regexPattern; }
    public Integer getMinLength() { return minLength; }
    public Integer getMaxLength() { return maxLength; }
    public String getErrorMessage() { return errorMessage; }
}
