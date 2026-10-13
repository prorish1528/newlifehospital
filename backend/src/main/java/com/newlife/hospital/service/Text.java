package com.newlife.hospital.service;

import java.util.Locale;

/** Small input normalization helpers applied before validation and persistence. */
final class Text {

    private Text() {
    }

    /** Trims and collapses internal whitespace; blank becomes {@code null}. */
    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    static String lower(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    static String upper(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toUpperCase(Locale.ROOT);
    }

    /** ID numbers are compared case-insensitively and without spaces or hyphens. */
    static String idNumber(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    /** Escapes LIKE wildcards with backslash so user input is matched literally. */
    static String likeContains(String value) {
        String escaped = value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
