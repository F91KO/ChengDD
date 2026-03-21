package com.cdd.common.core.runtime;

public enum RuntimeEnv {
    LOCAL,
    DEV,
    TEST,
    PROD;

    public static RuntimeEnv from(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        return RuntimeEnv.valueOf(value.trim().toUpperCase());
    }
}
