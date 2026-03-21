package com.cdd.release.domain;

import java.util.Locale;

public enum ReleaseStepStatus {
    PENDING("pending"),
    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed");

    private final String value;

    ReleaseStepStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean done() {
        return this == SUCCESS || this == FAILED;
    }

    public static ReleaseStepStatus from(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ReleaseStepStatus status : values()) {
            if (status.value.equals(normalized)) {
                return status;
            }
        }
        return null;
    }
}
