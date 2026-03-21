package com.cdd.release.domain;

public enum ResultSyncStatus {
    PENDING("pending"),
    SYNCED("synced"),
    FAILED("failed");

    private final String value;

    ResultSyncStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
