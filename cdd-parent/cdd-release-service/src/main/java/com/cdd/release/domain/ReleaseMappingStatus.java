package com.cdd.release.domain;

import java.util.Locale;

public enum ReleaseMappingStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    ReleaseMappingStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ReleaseMappingStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIVE;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ReleaseMappingStatus status : values()) {
            if (status.value.equals(normalized)) {
                return status;
            }
        }
        return null;
    }
}
