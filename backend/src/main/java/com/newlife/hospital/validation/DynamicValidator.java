package com.newlife.hospital.validation;

import com.newlife.hospital.domain.FieldValidation;
import com.newlife.hospital.exception.ApiException;
import com.newlife.hospital.repository.FieldValidationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Server-side half of the database-driven validation: applies the exact same {@code field_validations} rows that
 * the Angular client receives from {@code /api/validations}, so a request that bypasses the UI is held to the
 * same rules.
 */
@Component
public class DynamicValidator {

    private final FieldValidationRepository repository;
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public DynamicValidator(FieldValidationRepository repository) {
        this.repository = repository;
    }

    /** Starts a validation pass for one form; rules are read fresh so DB edits apply without a restart. */
    public FormCheck form(String formName) {
        Map<String, FieldValidation> rules = repository.findByFormNameOrderByIdAsc(formName).stream()
                .collect(Collectors.toMap(FieldValidation::getFieldName, Function.identity()));
        if (rules.isEmpty()) {
            throw new IllegalStateException("No validation rules configured for form " + formName);
        }
        return new FormCheck(formName, rules);
    }

    public final class FormCheck {
        private final String formName;
        private final Map<String, FieldValidation> rules;
        private final Map<String, String> errors = new LinkedHashMap<>();

        private FormCheck(String formName, Map<String, FieldValidation> rules) {
            this.formName = formName;
            this.rules = rules;
        }

        /** Validates {@code value} for {@code field} using the rule of the same name. */
        public FormCheck check(String field, Object value) {
            return check(field, field, value);
        }

        /** Validates {@code value}, reporting errors under {@code field} but using the rule named {@code ruleName}. */
        public FormCheck check(String field, String ruleName, Object value) {
            FieldValidation rule = rules.get(ruleName);
            if (rule == null) {
                throw new IllegalStateException("Missing validation rule " + formName + "." + ruleName);
            }
            if (!errors.containsKey(field) && !isValid(rule, value)) {
                errors.put(field, rule.getErrorMessage());
            }
            return this;
        }

        /** Adds a cross-field or business-rule error. */
        public FormCheck reject(String field, String message) {
            errors.putIfAbsent(field, message);
            return this;
        }

        public boolean hasRule(String ruleName) {
            return rules.containsKey(ruleName);
        }

        public boolean hasError(String field) {
            return errors.containsKey(field);
        }

        public void throwIfInvalid() {
            if (!errors.isEmpty()) {
                throw ApiException.invalidFields(errors);
            }
        }
    }

    boolean isValid(FieldValidation rule, Object rawValue) {
        String value = rawValue == null ? null : rawValue.toString();
        if (value == null || value.isBlank()) {
            return !rule.isRequired();
        }
        if (rule.getMinLength() != null && value.length() < rule.getMinLength()) {
            return false;
        }
        if (rule.getMaxLength() != null && value.length() > rule.getMaxLength()) {
            return false;
        }
        if (rule.getRegexPattern() != null && !rule.getRegexPattern().isBlank()) {
            Pattern pattern = patternCache.computeIfAbsent(rule.getRegexPattern(), Pattern::compile);
            if (!pattern.matcher(value).matches()) {
                return false;
            }
        }
        return switch (rule.getDataType()) {
            case NUMBER -> isLong(value);
            case DECIMAL -> isNonNegativeDecimal(value);
            case DATE -> isDate(value);
            case STRING, EMAIL, PHONE -> true;
        };
    }

    private static boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isNonNegativeDecimal(String value) {
        try {
            return new BigDecimal(value).signum() >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
