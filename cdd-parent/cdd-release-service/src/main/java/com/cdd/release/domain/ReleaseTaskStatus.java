package com.cdd.release.domain;

import java.util.Locale;
import java.util.Set;

public enum ReleaseTaskStatus {
    PENDING("pending"),
    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed"),
    ROLLING_BACK("rolling_back"),
    ROLLED_BACK("rolled_back");

    private final String value;

    ReleaseTaskStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean canTransitTo(ReleaseTaskStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case PENDING -> Set.of(RUNNING, FAILED).contains(target);
            case RUNNING -> Set.of(SUCCESS, FAILED, ROLLING_BACK).contains(target);
            case SUCCESS -> Set.of(ROLLING_BACK).contains(target);
            case FAILED -> Set.of(ROLLING_BACK).contains(target);
            case ROLLING_BACK -> Set.of(ROLLED_BACK, FAILED).contains(target);
            case ROLLED_BACK -> false;
        };
    }

    public boolean terminal() {
        return this == SUCCESS || this == FAILED || this == ROLLED_BACK;
    }

    public static ReleaseTaskStatus from(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ReleaseTaskStatus status : values()) {
            if (status.value.equals(normalized)) {
                return status;
            }
        }
        return null;
    }
}
