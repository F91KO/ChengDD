package com.cdd.common.core.runtime;

public enum ConfigMode {
    FILE,
    NACOS;

    public static ConfigMode from(String value) {
        if (value == null || value.isBlank()) {
            return FILE;
        }
        return ConfigMode.valueOf(value.trim().toUpperCase());
    }
}
